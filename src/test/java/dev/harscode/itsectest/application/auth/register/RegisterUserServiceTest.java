package dev.harscode.itsectest.application.auth.register;

import dev.harscode.itsectest.config.AuthProperties;
import dev.harscode.itsectest.domain.auth.UserToken;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.crypt.PasswordHasher;
import dev.harscode.itsectest.ports.mail.MailSenderPort;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserTokenRepository;
import dev.harscode.itsectest.security.token.TokenHashServiceImpl;
import dev.harscode.itsectest.security.token.VerificationTokenGenerator;
import dev.harscode.itsectest.web.exception.UnprocessableEntityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private VerificationTokenGenerator tokenGenerator;

    @Mock
    private TokenHashServiceImpl tokenHashService;

    @Mock
    private MailSenderPort mailSender;

    @Mock
    private AuthProperties authProperties;

    @InjectMocks
    private RegisterUserService service;

    private RegisterUserCommand cmd;

    @BeforeEach
    void setUp() {
        cmd = new RegisterUserCommand(
                "JohnDoe ",
                " John.Doe@Example.com ",
                "Secret123!",
                "John Doe"
        );
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyTaken() {
        String normalizedUsername = "johndoe";
        String normalizedEmail = "john.doe@example.com";

        when(userRepository.existsByUsername(normalizedUsername)).thenReturn(true);

        assertThatThrownBy(() -> service.register(cmd))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Username already taken");

        verify(userRepository).existsByUsername(normalizedUsername);
        verify(userRepository, never()).existsByEmail(normalizedEmail);
        verifyNoInteractions(passwordHasher, userProfileRepository, userTokenRepository, tokenGenerator, tokenHashService, mailSender);
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyRegistered() {
        String normalizedUsername = "johndoe";
        String normalizedEmail = "john.doe@example.com";

        when(userRepository.existsByUsername(normalizedUsername)).thenReturn(false);
        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(true);

        assertThatThrownBy(() -> service.register(cmd))
                .isInstanceOf(UnprocessableEntityException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository).existsByUsername(normalizedUsername);
        verify(userRepository).existsByEmail(normalizedEmail);
        verifyNoInteractions(passwordHasher, userProfileRepository, userTokenRepository, tokenGenerator, tokenHashService, mailSender);
    }

    @Test
    void register_shouldCreateUserProfileTokenAndSendEmail() {
        String normalizedUsername = "johndoe";
        String normalizedEmail = "john.doe@example.com";
        String hashedPassword = "hashed-secret";
        UUID generatedUserId = UUID.randomUUID();

        when(userRepository.existsByUsername(normalizedUsername)).thenReturn(false);
        when(userRepository.existsByEmail(normalizedEmail)).thenReturn(false);

        when(passwordHasher.hash("Secret123!")).thenReturn(hashedPassword);

        // userRepository.save: set ID and return
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(generatedUserId);
                    return u;
                });

        when(tokenGenerator.generateRandomToken()).thenReturn("plain-token");
        when(tokenHashService.hash("plain-token")).thenReturn("hashed-token");

//        when(authProperties.getEmailVerificationTtlHours()).thenReturn(24);
        when(authProperties.getEmailVerificationBaseUrl()).thenReturn("https://example.com/verify");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        ArgumentCaptor<UserProfile> profileCaptor = ArgumentCaptor.forClass(UserProfile.class);
        ArgumentCaptor<UserToken> tokenCaptor = ArgumentCaptor.forClass(UserToken.class);

        when(userTokenRepository.create(tokenCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterUserResult result = service.register(cmd);

        // Assert result
        assertThat(result.userId()).isEqualTo(generatedUserId);
        assertThat(result.username()).isEqualTo(normalizedUsername);
        assertThat(result.email()).isEqualTo(normalizedEmail);
        assertThat(result.role()).isEqualTo("viewer");
        assertThat(result.status()).isEqualTo("inactive");

        // Verify user saved with normalized values
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo(normalizedUsername);
        assertThat(savedUser.getEmail()).isEqualTo(normalizedEmail);
        assertThat(savedUser.getPasswordHash()).isEqualTo(hashedPassword);
        assertThat(savedUser.getRole()).isEqualTo("viewer");
        assertThat(savedUser.getStatus()).isEqualTo("inactive");
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.isMfaEnabled()).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();
        assertThat(savedUser.getUpdatedAt()).isNotNull();

        // Verify profile
        verify(userProfileRepository).save(profileCaptor.capture());
        UserProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getUserId()).isEqualTo(generatedUserId);
        assertThat(savedProfile.getFullName()).isEqualTo("John Doe");
        assertThat(savedProfile.getCreatedAt()).isNotNull();
        assertThat(savedProfile.getUpdatedAt()).isNotNull();

        // Verify token
        UserToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(generatedUserId);
        assertThat(savedToken.getTokenHash()).isEqualTo("hashed-token");
        assertThat(savedToken.getTokenType()).isEqualTo("email-verification");
//        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now());
        assertThat(savedToken.getCreatedAt()).isNotNull();
        assertThat(savedToken.getUpdatedAt()).isNotNull();

        // Verify email sent
        String expectedLink = "https://example.com/verify?token=" + savedToken.getTokenHash();
        verify(mailSender).sendEmailVerification(normalizedEmail, expectedLink);
    }
}