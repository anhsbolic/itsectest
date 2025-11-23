package dev.harscode.itsectest.application.auth.mfa;

import dev.harscode.itsectest.application.auditlog.AuditLogger;
import dev.harscode.itsectest.application.auth.login.LoginUserResult;
import dev.harscode.itsectest.domain.auth.MfaOtpSession;
import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.mail.MailSenderPort;
import dev.harscode.itsectest.ports.repository.MfaOtpRepository;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserSessionRepository;
import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.security.otp.OtpGenerator;
import dev.harscode.itsectest.security.otp.OtpRateLimiter;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.AuthenticationException;
import dev.harscode.itsectest.web.exception.TooManyOtpAttemptsException;
import dev.harscode.itsectest.web.exception.TooManyOtpRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaOtpServiceTest {

    @Mock
    private MfaOtpRepository mfaOtpRepository;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private OtpGenerator otpGenerator;

    @Mock
    private OtpRateLimiter otpRateLimiter;

    @Mock
    private MailSenderPort mailSenderPort;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private MfaOtpService service;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setEmail("user@example.com");
        user.setUsername("user");
        user.setRole("editor");
    }

    @Test
    void startMfaForLogin_shouldCreateSessionSendEmailAndAudit_whenUnderGlobalLimit() {
        String userAgent = "JUnit-UA";
        String ip = "127.0.0.1";

        when(otpRateLimiter.incrementUserOtpRequests(eq(userId), any()))
                .thenReturn(1L);

        when(otpGenerator.generateNumericOtp(6)).thenReturn("123456");
        when(tokenHashService.hash("123456")).thenReturn("otp-hash");
        when(tokenHashService.hash(userAgent)).thenReturn("ua-hash");
        when(tokenHashService.hash(ip)).thenReturn("ip-hash");

        when(mfaOtpRepository.create(any(MfaOtpSession.class)))
                .thenAnswer(invocation -> {
                    MfaOtpSession s = invocation.getArgument(0);
                    s.setId("session-123");
                    return s;
                });

        String sessionId = service.startMfaForLogin(user, userAgent, ip);

        assertThat(sessionId).isEqualTo("session-123");

        verify(otpRateLimiter).incrementUserOtpRequests(eq(userId), any());
        verify(otpGenerator).generateNumericOtp(6);
        verify(tokenHashService).hash("123456");
        verify(mfaOtpRepository).create(any(MfaOtpSession.class));
        verify(mailSenderPort).sendMfaOtp("user@example.com", "123456");
        verify(auditLogger).loginOtpSent(eq(userId), eq(ip), eq("ip-hash"), eq(userAgent), eq("ua-hash"));
    }

    @Test
    void startMfaForLogin_shouldThrowTooManyOtpRequests_whenGlobalLimitExceeded() {
        when(otpRateLimiter.incrementUserOtpRequests(eq(userId), any()))
                .thenReturn(10L);

        assertThatThrownBy(() -> service.startMfaForLogin(user, "JUnit-UA", "127.0.0.1"))
                .isInstanceOf(TooManyOtpRequestsException.class);

        verifyNoInteractions(otpGenerator);
        verifyNoInteractions(mfaOtpRepository);
        verifyNoInteractions(mailSenderPort);
    }

    @Test
    void verify_shouldReturnTokensAndDeleteSession_whenOtpValid() {
        String sessionId = "sess-1";
        String otp = "654321";
        String ua = "JUnit-UA";
        String ip = "127.0.0.1";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.canAttemptMore()).thenReturn(true);
        when(session.getUserId()).thenReturn(userId);
        when(session.getOtpHash()).thenReturn("otp-hash");

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setFullName("User Name");
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(tokenHashService.hash(otp)).thenReturn("otp-hash");
        when(tokenHashService.hash(ua)).thenReturn("ua-hash");
        when(tokenHashService.hash(ip)).thenReturn("ip-hash");
        when(tokenHashService.generateRefreshToken()).thenReturn("raw-refresh");
        when(tokenHashService.hash("raw-refresh")).thenReturn("refresh-hash");

        UserSession created = new UserSession();
        created.setId(UUID.randomUUID());
        when(userSessionRepository.create(any(UserSession.class))).thenReturn(created);

        when(jwtTokenService.generateAccessToken(
                anyString(), anyString(), anyString(), anyBoolean()
        )).thenReturn("access-token");

        LoginUserResult result = service.verify(new VerifyMfaCommand(sessionId, otp, ua, ip));

        assertThat(result.mfaRequired()).isFalse();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshTokenRaw()).isEqualTo("raw-refresh");
        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.profile().getFullName()).isEqualTo("User Name");

        verify(mfaOtpRepository).delete(sessionId);
        verify(userSessionRepository).create(any(UserSession.class));
        verify(jwtTokenService).generateAccessToken(
                eq(userId.toString()),
                anyString(),
                eq(user.getRole()),
                eq(false)
        );
        verify(auditLogger).loginOtpSuccess(eq(userId), eq(ip), eq("ip-hash"), eq(ua), eq("ua-hash"));
        verify(auditLogger, never()).loginOtpFailed(any(), any(), any(), any(), any(), any());
    }

    @Test
    void verify_shouldThrowAuthenticationException_whenSessionNotFound() {
        when(mfaOtpRepository.findById("sess-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.verify(new VerifyMfaCommand("sess-1", "123456", "UA", "IP"))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("MFA session not found");

        verifyNoInteractions(userRepository);
    }

    @Test
    void verify_shouldDeleteSessionAndThrow_whenSessionExpired() {
        String sessionId = "sess-expired";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(true);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() ->
                service.verify(new VerifyMfaCommand(sessionId, "123456", "UA", "IP"))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("MFA session expired");

        verify(mfaOtpRepository).delete(sessionId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void verify_shouldDeleteSessionAndThrow_whenUserNotFound() {
        String sessionId = "sess-user-missing";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.getUserId()).thenReturn(userId);
//        when(session.canAttemptMore()).thenReturn(true);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.verify(new VerifyMfaCommand(sessionId, "123456", "UA", "IP"))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("User not found");

        verify(mfaOtpRepository).delete(sessionId);
    }

    @Test
    void verify_shouldThrowTooManyOtpAttempts_whenCannotAttemptMore() {
        String sessionId = "sess-max-attempts";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.canAttemptMore()).thenReturn(false);
        when(session.getUserId()).thenReturn(userId);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                service.verify(new VerifyMfaCommand(sessionId, "123456", "UA", "IP"))
        ).isInstanceOf(TooManyOtpAttemptsException.class);

        verify(mfaOtpRepository).delete(sessionId);
    }

    @Test
    void verify_shouldIncreaseAttemptsAndAudit_whenOtpInvalid() {
        String sessionId = "sess-invalid-otp";
        String otp = "000000";
        String ua = "JUnit-UA";
        String ip = "127.0.0.1";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.canAttemptMore()).thenReturn(true);
        when(session.getUserId()).thenReturn(userId);
        when(session.getOtpHash()).thenReturn("correct-hash");
        when(session.getAttempts()).thenReturn(0);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(tokenHashService.hash(otp)).thenReturn("wrong-hash");
        when(tokenHashService.hash(ua)).thenReturn("ua-hash");
        when(tokenHashService.hash(ip)).thenReturn("ip-hash");

        assertThatThrownBy(() ->
                service.verify(new VerifyMfaCommand(sessionId, otp, ua, ip))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid OTP");

        verify(session).setAttempts(1);
        verify(mfaOtpRepository).save(session);
        verify(auditLogger).loginOtpFailed(eq(userId), eq(ip), eq("ip-hash"), eq(ua), eq("ua-hash"), anyString());
        verify(mfaOtpRepository, never()).delete(sessionId);
    }

    @Test
    void resend_shouldUpdateOtpAndSendEmailAndAudit_whenWithinLimits() {
        String sessionId = "sess-resend";
        String ua = "JUnit-UA";
        String ip = "127.0.0.1";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.getUserId()).thenReturn(userId);
        when(session.canRequestMore()).thenReturn(true);
        when(session.getRequests()).thenReturn(1);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(otpRateLimiter.incrementUserOtpRequests(eq(userId), any()))
                .thenReturn(2L);

        when(otpGenerator.generateNumericOtp(6)).thenReturn("999999");
        when(tokenHashService.hash("999999")).thenReturn("otp-hash");
        when(tokenHashService.hash(ua)).thenReturn("ua-hash");
        when(tokenHashService.hash(ip)).thenReturn("ip-hash");

        service.resend(new ResendMfaCommand(sessionId, ua, ip));

        verify(session).setOtpHash("otp-hash");
        verify(session).setRequests(2);
        verify(mfaOtpRepository).save(session);
        verify(mailSenderPort).sendMfaOtp("user@example.com", "999999");
        verify(auditLogger).loginOtpResend(eq(userId), eq(ip), eq("ip-hash"), eq(ua), eq("ua-hash"));
    }

    @Test
    void resend_shouldThrowAuthenticationException_whenSessionNotFound() {
        when(mfaOtpRepository.findById("sess-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.resend(new ResendMfaCommand("sess-x", "UA", "IP"))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("MFA session not found");

        verifyNoInteractions(userRepository);
    }

    @Test
    void resend_shouldDeleteSessionAndThrow_whenSessionExpired() {
        String sessionId = "sess-expired";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(true);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() ->
                service.resend(new ResendMfaCommand(sessionId, "UA", "IP"))
        ).isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("MFA session expired");

        verify(mfaOtpRepository).delete(sessionId);
        verifyNoInteractions(userRepository);
    }

    @Test
    void resend_shouldThrowTooManyOtpRequests_whenSessionRequestLimitReached() {
        String sessionId = "sess-max-req";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.canRequestMore()).thenReturn(false);
        when(session.getUserId()).thenReturn(userId);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                service.resend(new ResendMfaCommand(sessionId, "UA", "IP"))
        ).isInstanceOf(TooManyOtpRequestsException.class)
                .hasMessageContaining("Maximum OTP resend");

        verify(otpRateLimiter, never()).incrementUserOtpRequests(any(), any());
        verifyNoInteractions(otpGenerator);
    }

    @Test
    void resend_shouldThrowTooManyOtpRequests_whenGlobalLimitExceeded() {
        String sessionId = "sess-global-max";

        MfaOtpSession session = mock(MfaOtpSession.class);
        when(session.isExpired()).thenReturn(false);
        when(session.canRequestMore()).thenReturn(true);
        when(session.getUserId()).thenReturn(userId);
//        when(session.getRequests()).thenReturn(1);

        when(mfaOtpRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(otpRateLimiter.incrementUserOtpRequests(eq(userId), any()))
                .thenReturn(10L);

        assertThatThrownBy(() ->
                service.resend(new ResendMfaCommand(sessionId, "UA", "IP"))
        ).isInstanceOf(TooManyOtpRequestsException.class);

        verifyNoInteractions(otpGenerator);
        verify(mfaOtpRepository, never()).save(any());
        verifyNoInteractions(mailSenderPort);
    }
}