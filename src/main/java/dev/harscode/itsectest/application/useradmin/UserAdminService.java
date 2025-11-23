package dev.harscode.itsectest.application.useradmin;

import dev.harscode.itsectest.adapters.web.user.dto.CreateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UpdateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UserAdminResult;
import dev.harscode.itsectest.application.auditlog.AuditLogger;
import dev.harscode.itsectest.application.metadata.RequestMetadata;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.crypt.PasswordHasher;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;

import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.NotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class UserAdminService implements UserAdminUsecase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHashService tokenHashService;
    private final AuditLogger auditLogger;

    public UserAdminService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            PasswordHasher passwordHasher,
            TokenHashService tokenHashService,
            AuditLogger auditLogger
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHashService = tokenHashService;
        this.auditLogger = auditLogger;
    }

    @Override
    public List<UserAdminResult> list(int page, int size) {
        var users = userRepository.findPage(page, size);
        return users.stream()
                .map(u -> {
                    var profile = userProfileRepository.findByUserId(u.getId()).orElse(null);
                    return new UserAdminResult(u, profile);
                })
                .toList();
    }

    @Override
    public UserAdminResult get(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);

        return new UserAdminResult(user, profile);
    }

    @Override
    @Transactional
    public UserAdminResult create(CreateUserCommand cmd, RequestMetadata metadata) {
        String usernameNorm = cmd.username().trim().toLowerCase();
        String emailNorm = cmd.email().trim().toLowerCase();

        String passwordHash = passwordHasher.hash(cmd.password());

        User user = new User();
        user.setUsername(usernameNorm);
        user.setEmail(emailNorm);
        user.setPasswordHash(passwordHash);
        user.setRole(cmd.role());
        user.setStatus("active");
        user.setEmailVerified(true);
        user.setMfaEnabled(true);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User createdUser = userRepository.create(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(createdUser.getId());
        profile.setFullName(cmd.fullName());
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        userProfileRepository.save(profile);

        auditLogger.log(
                UUID.fromString(metadata.actorId()),
                "USER_CREATE",
                "User",
                user.getId(),
                true,
                metadata.ip(),
                tokenHashService.hash(metadata.ip()),
                metadata.userAgent(),
                tokenHashService.hash(metadata.userAgent()),
                "Created user: " + user.getUsername()
        );

        return new UserAdminResult(createdUser, profile);
    }

    @Override
    @Transactional
    public UserAdminResult update(UUID userId, UpdateUserCommand cmd, RequestMetadata metadata) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User not found")
        );

        // email (optional)
        if (cmd.email() != null && !cmd.email().isBlank()) {
            String emailNorm = cmd.email().trim().toLowerCase();
            user.setEmail(emailNorm);
        }

        // role (optional)
        if (cmd.role() != null && !cmd.role().isBlank()) {
            user.setRole(cmd.role());
        }

        // status (optional)
        if (cmd.status() != null && !cmd.status().isBlank()) {
            user.setStatus(cmd.status());
        }

        // mfaEnabled (optional)
        if (cmd.mfaEnabled() != null) {
            user.setMfaEnabled(cmd.mfaEnabled());
        }

        user.setUpdatedAt(Instant.now());
        user = userRepository.update(user);

        // profile
        UserProfile profile = userProfileRepository.findByUserId(userId).orElseGet(() -> {
            UserProfile p = new UserProfile();
            p.setUserId(userId);
            p.setCreatedAt(Instant.now());
            return p;
        });

        if (cmd.fullName() != null && !cmd.fullName().isBlank()) {
            profile.setFullName(cmd.fullName());
        }

        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);

        auditLogger.log(
                UUID.fromString(metadata.actorId()),
                "USER_UPDATE",
                "User",
                user.getId(),
                true,
                metadata.ip(),
                tokenHashService.hash(metadata.ip()),
                metadata.userAgent(),
                tokenHashService.hash(metadata.userAgent()),
                "Updated user: " + user.getUsername()
        );

        return new UserAdminResult(user, profile);
    }

    @Override
    @Transactional
    public void delete(UUID userId, RequestMetadata metadata) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new NotFoundException("User not found")
        );

        auditLogger.log(
                UUID.fromString(metadata.actorId()),
                "USER_DELETE",
                "User",
                user.getId(),
                true,
                metadata.ip(),
                tokenHashService.hash(metadata.ip()),
                metadata.userAgent(),
                tokenHashService.hash(metadata.userAgent()),
                "Deleted user: " + user.getUsername()
        );

        userRepository.softDelete(user.getId());
    }
}
