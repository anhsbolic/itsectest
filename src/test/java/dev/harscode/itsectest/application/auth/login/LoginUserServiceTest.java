package dev.harscode.itsectest.application.auth.login;

import dev.harscode.itsectest.application.auth.mfa.MfaOtpService;
import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserSessionRepository;
import dev.harscode.itsectest.ports.service.LoginAttemptService;
import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private MfaOtpService mfaOtpService;

    @InjectMocks
    private LoginUserService service;

    private UUID userId;
    private UUID sessionId;

    private final String RAW_PASSWORD = "Secret123!";
    private final String ENCODED_PASSWORD = "encoded-secret";
    private final String USERNAME_OR_EMAIL = "User@Test.com";
    private final String USER_AGENT = "JUnit-UA";
    private final String IP = "127.0.0.1";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        when(tokenHashService.hash(anyString()))
                .thenAnswer(invocation -> "hash-" + invocation.getArgument(0));

//        when(userProfileRepository.findByUserId(any()))
//                .thenReturn(Optional.empty());
    }

    private User makeActiveUser(boolean mfaEnabled) {
        User u = new User();
        u.setId(userId);
        u.setUsername("user");
        u.setEmail("user@test.com");
        u.setPasswordHash(ENCODED_PASSWORD);
        u.setStatus("active");
        u.setEmailVerified(true);
        u.setMfaEnabled(mfaEnabled);
        u.setRole("viewer");
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }

    private UserProfile makeProfile() {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setFullName("JUnit User");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    private LoginUserCommand makeCommand() {
        return new LoginUserCommand(
                USERNAME_OR_EMAIL,
                RAW_PASSWORD,
                USER_AGENT,
                IP
        );
    }

    @Test
    void login_shouldReturnSuccessResult_andCreateSession_whenMfaDisabled() {
        User user = makeActiveUser(false);
        UserProfile profile = makeProfile();

        when(userRepository.findByUsernameOrEmail(USERNAME_OR_EMAIL.toLowerCase()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);
        when(userProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(profile));

        when(tokenHashService.generateRefreshToken())
                .thenReturn("refresh-raw");
        when(jwtTokenService.generateAccessToken(
                anyString(), anyString(), anyString(), anyBoolean())
        ).thenReturn("access-token");

        when(userSessionRepository.create(any(UserSession.class)))
                .thenAnswer(invocation -> {
                    UserSession s = invocation.getArgument(0);
                    s.setId(sessionId);
                    return s;
                });

        LoginUserCommand cmd = makeCommand();

        LoginUserResult result = service.login(cmd);

        assertThat(result).isNotNull();
        assertThat(result.mfaRequired()).isFalse();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshTokenRaw()).isEqualTo("refresh-raw");
        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.profile().getFullName()).isEqualTo("JUnit User");

        String attemptKey = "hash-" + USERNAME_OR_EMAIL.toLowerCase();

        verify(loginAttemptService).assertNotBlocked(attemptKey);
        verify(loginAttemptService).reset(attemptKey);

        verify(userSessionRepository).create(any(UserSession.class));
        verify(jwtTokenService).generateAccessToken(
                eq(userId.toString()),
                eq(sessionId.toString()),
                eq("viewer"),
                eq(false)
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(AuditLog::getActivity)
                .containsExactly("LOGIN_ATTEMPT", "LOGIN_SUCCESS");
    }

    @Test
    void login_shouldReturnMfaRequired_whenMfaEnabled() {
        User user = makeActiveUser(true);
        when(userRepository.findByUsernameOrEmail(USERNAME_OR_EMAIL.toLowerCase()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(true);

        when(mfaOtpService.startMfaForLogin(user, USER_AGENT, IP))
                .thenReturn("mfa-session-123");

        LoginUserCommand cmd = makeCommand();

        LoginUserResult result = service.login(cmd);

        assertThat(result).isNotNull();
        assertThat(result.mfaRequired()).isTrue();
        assertThat(result.mfaSessionId()).isEqualTo("mfa-session-123");

        verify(userSessionRepository, never()).create(any());
        verify(jwtTokenService, never()).generateAccessToken(any(), any(), any(), anyBoolean());
        verify(loginAttemptService, never()).reset(anyString());

        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    void login_shouldThrowUnauthorized_andRecordFailure_whenUserNotFound() {
        when(userRepository.findByUsernameOrEmail(USERNAME_OR_EMAIL.toLowerCase()))
                .thenReturn(Optional.empty());

        LoginUserCommand cmd = makeCommand();

        assertThatThrownBy(() -> service.login(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");

        String attemptKey = "hash-" + USERNAME_OR_EMAIL.toLowerCase();

        verify(loginAttemptService).assertNotBlocked(attemptKey);
        verify(loginAttemptService).recordFailure(attemptKey);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(AuditLog::getActivity)
                .containsExactly("LOGIN_ATTEMPT", "LOGIN_FAILED");
    }

    @Test
    void login_shouldThrowUnauthorized_whenUserInactiveOrEmailNotVerified() {
        User user = makeActiveUser(false);
        user.setStatus("inactive");

        when(userRepository.findByUsernameOrEmail(USERNAME_OR_EMAIL.toLowerCase()))
                .thenReturn(Optional.of(user));

        LoginUserCommand cmd = makeCommand();

        assertThatThrownBy(() -> service.login(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not active or email not verified");

        String attemptKey = "hash-" + USERNAME_OR_EMAIL.toLowerCase();

        verify(loginAttemptService).assertNotBlocked(attemptKey);
        verify(loginAttemptService, never()).recordFailure(anyString());
        verify(auditLogRepository, atLeast(2)).save(any(AuditLog.class));
    }

    @Test
    void login_shouldThrowUnauthorized_andRecordFailure_whenPasswordInvalid() {
        User user = makeActiveUser(false);

        when(userRepository.findByUsernameOrEmail(USERNAME_OR_EMAIL.toLowerCase()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD))
                .thenReturn(false);

        LoginUserCommand cmd = makeCommand();

        assertThatThrownBy(() -> service.login(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");

        String attemptKey = "hash-" + USERNAME_OR_EMAIL.toLowerCase();

        verify(loginAttemptService).assertNotBlocked(attemptKey);
        verify(loginAttemptService).recordFailure(attemptKey);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(AuditLog::getActivity)
                .containsExactly("LOGIN_ATTEMPT", "LOGIN_FAILED");
    }

    @Test
    void login_shouldPropagateException_whenUserBlockedByLoginAttemptService() {
        LoginUserCommand cmd = makeCommand();

        String attemptKey = "hash-" + USERNAME_OR_EMAIL.toLowerCase();

        doThrow(new UnauthorizedException("Too many attempts"))
                .when(loginAttemptService).assertNotBlocked(attemptKey);

        assertThatThrownBy(() -> service.login(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Too many attempts");

        verify(userRepository, never()).findByUsernameOrEmail(anyString());
    }
}