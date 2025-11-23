package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.config.AuthProperties;
import dev.harscode.itsectest.domain.auth.UserToken;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.*;
import dev.harscode.itsectest.security.token.TokenHashServiceImpl;
import dev.harscode.itsectest.security.token.VerificationTokenGenerator;
import dev.harscode.itsectest.web.exception.UnprocessableEntityException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RegisterUserService implements RegisterUserUsecase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PasswordHasher passwordHasher;
    private final UserTokenRepository userTokenRepository;
    private final VerificationTokenGenerator tokenGenerator;
    private final TokenHashServiceImpl tokenHashService;
    private final MailSenderPort mailSender;
    private final AuthProperties authProperties;

    public RegisterUserService(UserRepository userRepository,
                               UserProfileRepository userProfileRepository,
                               PasswordHasher passwordHasher,
                               UserTokenRepository userTokenRepository,
                               VerificationTokenGenerator tokenGenerator,
                               TokenHashServiceImpl tokenHashService,
                               MailSenderPort mailSender,
                               AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.passwordHasher = passwordHasher;
        this.userTokenRepository = userTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHashService = tokenHashService;
        this.mailSender = mailSender;
        this.authProperties = authProperties;
    }

    @Override
    @Transactional
    public RegisterUserResult register(RegisterUserCommand cmd) {
        String username = cmd.username().trim().toLowerCase();
        String email = cmd.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new UnprocessableEntityException("Username already taken");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UnprocessableEntityException("Email already registered");
        }

        // Create user
        String passwordHash = passwordHasher.hash(cmd.password());
        Instant now = Instant.now();

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setStatus("inactive");
        user.setRole("viewer");
        user.setEmailVerified(false);
        user.setMfaEnabled(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User savedUser = userRepository.save(user);

        // Create profile
        UserProfile profile = new UserProfile();
        profile.setUserId(savedUser.getId());
        profile.setFullName(cmd.name().trim());
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        userProfileRepository.save(profile);

        // Generate verification token
        String plainToken = tokenGenerator.generateRandomToken();
        String hash = tokenHashService.hash(plainToken);

        Instant expiresAt = Instant.now().plus(authProperties.getEmailVerificationTtlHours(), ChronoUnit.HOURS);

        UserToken token = new UserToken();
        token.setUserId(savedUser.getId());
        token.setTokenHash(hash);
        token.setTokenType("email-verification");
        token.setExpiresAt(expiresAt);
        token.setCreatedAt(now);
        token.setUpdatedAt(now);

        userTokenRepository.create(token);

        // Send Email
        String verificationUrl = authProperties.getEmailVerificationBaseUrl();
        String link = verificationUrl + "?token=" + token.getTokenHash();
        mailSender.sendEmailVerification(user.getEmail(), link);

        return new RegisterUserResult(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getStatus()
        );
    }
}