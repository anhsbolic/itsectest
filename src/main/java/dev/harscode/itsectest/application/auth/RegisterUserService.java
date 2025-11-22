package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.PasswordHasher;
import dev.harscode.itsectest.ports.UserProfileRepository;
import dev.harscode.itsectest.ports.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RegisterUserService implements RegisterUserUsecase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordHasher passwordHasher;

    public RegisterUserService(UserRepository userRepository,
                               UserProfileRepository userProfileRepository,
                               PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand cmd) {
        String username = cmd.username().trim().toLowerCase();
        String email = cmd.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        String passwordHash = passwordHasher.hash(cmd.password());
        Instant now = Instant.now();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus("inactive");
        user.setRole("viewer");
        user.setEmailVerified(false);
        user.setMfaEnabled(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(savedUser.getId());
        profile.setFullName(cmd.name().trim());
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        userProfileRepository.save(profile);

        return new RegisterUserResult(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }
}