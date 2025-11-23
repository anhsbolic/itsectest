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
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class MfaOtpService implements VerifyMfaUsecase, ResendMfaUsecase {
    private static final int OTP_TTL_SECONDS = 300;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_SESSION = 3;
    private static final int GLOBAL_MAX_REQUESTS = 5;
    private static final Duration GLOBAL_WINDOW = Duration.ofMinutes(10);

    private final MfaOtpRepository mfaOtpRepository;
    private final TokenHashService tokenHashService;
    private final OtpGenerator otpGenerator;
    private final OtpRateLimiter otpRateLimiter;
    private final MailSenderPort mailSenderPort;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenService jwtTokenService;
    private final AuditLogger auditLogger;

    public MfaOtpService(
            MfaOtpRepository mfaOtpRepository,
            TokenHashService tokenHashService,
            OtpGenerator otpGenerator,
            OtpRateLimiter otpRateLimiter,
            MailSenderPort mailSenderPort,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserSessionRepository userSessionRepository,
            JwtTokenService jwtTokenService,
            AuditLogger auditLogger
    ) {
        this.mfaOtpRepository = mfaOtpRepository;
        this.tokenHashService = tokenHashService;
        this.otpGenerator = otpGenerator;
        this.otpRateLimiter = otpRateLimiter;
        this.mailSenderPort = mailSenderPort;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userSessionRepository = userSessionRepository;
        this.jwtTokenService = jwtTokenService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public String startMfaForLogin(User user, String userAgent, String ipAddress) {
        long globalCount = otpRateLimiter.incrementUserOtpRequests(user.getId(), GLOBAL_WINDOW);
        if (globalCount > GLOBAL_MAX_REQUESTS) {
            throw new TooManyOtpRequestsException("Too many MFA OTP requests, please try again later");
        }

        String otp = otpGenerator.generateNumericOtp(6);
        String otpHash = tokenHashService.hash(otp);

        String uaHash = tokenHashService.hash(userAgent == null ? "" : userAgent);
        String ipHash = tokenHashService.hash(ipAddress == null ? "" : ipAddress);

        Instant now = Instant.now();
        Instant expires = now.plusSeconds(OTP_TTL_SECONDS);

        MfaOtpSession session = new MfaOtpSession();
        session.setUserId(user.getId());
        session.setOtpHash(otpHash);
        session.setAttempts(0);
        session.setMaxAttempts(MAX_ATTEMPTS);
        session.setRequests(1);
        session.setMaxRequests(MAX_REQUESTS_PER_SESSION);
        session.setCreatedAt(now);
        session.setExpiresAt(expires);
        session.setUaHash(uaHash);
        session.setIpHash(ipHash);

        MfaOtpSession saved = mfaOtpRepository.create(session);

        mailSenderPort.sendMfaOtp(user.getEmail(), otp);

        auditLogger.loginOtpSent(user.getId(), ipAddress, ipHash, userAgent, uaHash);

        return saved.getId();
    }

    @Override
    @Transactional
    public LoginUserResult verify(VerifyMfaCommand cmd) {
        String sessionId = cmd.mfaSessionId().trim();
        String otp = cmd.otp().trim();
        String ua = cmd.userAgent() == null ? "" : cmd.userAgent();
        String ip = cmd.ipAddress() == null ? "" : cmd.ipAddress();

        MfaOtpSession session = mfaOtpRepository.findById(sessionId)
                .orElseThrow(() -> new AuthenticationException("MFA_SESSION_NOT_FOUND", "MFA session not found"));

        if (session.isExpired()) {
            mfaOtpRepository.delete(sessionId);
            throw new AuthenticationException("MFA_SESSION_EXPIRED", "MFA session expired");
        }

        var userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            mfaOtpRepository.delete(sessionId);
            throw new AuthenticationException("USER_NOT_FOUND", "User not found for MFA session");
        }
        User user = userOpt.get();
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        if (!session.canAttemptMore()) {
            mfaOtpRepository.delete(sessionId);
            throw new TooManyOtpAttemptsException("Too many invalid OTP attempts");
        }

        String otpHash = tokenHashService.hash(otp);

        String uaHash = tokenHashService.hash(ua);
        String ipHash = tokenHashService.hash(ip);

        if (!otpHash.equals(session.getOtpHash())) {
            session.setAttempts(session.getAttempts() + 1);
            mfaOtpRepository.save(session);

            auditLogger.loginOtpFailed(user.getId(), ip, ipHash, ua, uaHash, "Invalid MFA OTP");

            throw new AuthenticationException("MFA_OTP_INVALID", "Invalid OTP");
        }

        // OTP valid
        mfaOtpRepository.delete(sessionId);

        // create session & tokens persis seperti login sukses
        String refreshTokenRaw = tokenHashService.generateRefreshToken();
        String refreshTokenHash = tokenHashService.hash(refreshTokenRaw);

        UserSession userSession = new UserSession();
        userSession.setUserId(user.getId());
        userSession.setSessionId(UUID.randomUUID());
        userSession.setRefreshTokenHash(refreshTokenHash);
        userSession.setUserAgentHash(uaHash);
        userSession.setIpAddressHash(ipHash);
        userSession.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        UserSession createdSession = userSessionRepository.create(userSession);

        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(),
                createdSession.getId().toString(),
                user.getRole(),
                false
        );

        auditLogger.loginOtpSuccess(user.getId(), ip, ipHash, ua, uaHash);

        return new LoginUserResult(
                false,
                null,
                user,
                profile,
                accessToken,
                refreshTokenRaw
        );
    }

    @Override
    @Transactional
    public void resend(ResendMfaCommand cmd) {
        String sessionId = cmd.mfaSessionId().trim();
        String ua = cmd.userAgent() == null ? "" : cmd.userAgent();
        String ip = cmd.ipAddress() == null ? "" : cmd.ipAddress();

        MfaOtpSession session = mfaOtpRepository.findById(sessionId)
                .orElseThrow(() -> new AuthenticationException("MFA_SESSION_NOT_FOUND", "MFA session not found"));

        if (session.isExpired()) {
            mfaOtpRepository.delete(sessionId);
            throw new AuthenticationException("MFA_SESSION_EXPIRED", "MFA session expired");
        }

        var userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            mfaOtpRepository.delete(sessionId);
            throw new AuthenticationException("USER_NOT_FOUND", "User not found for MFA session");
        }
        User user = userOpt.get();

        if (!session.canRequestMore()) {
            throw new TooManyOtpRequestsException("Maximum OTP resend reached for this session");
        }

        long globalCount = otpRateLimiter.incrementUserOtpRequests(user.getId(), GLOBAL_WINDOW);
        if (globalCount > GLOBAL_MAX_REQUESTS) {
            throw new TooManyOtpRequestsException("Too many MFA OTP requests, please try again later");
        }

        String otp = otpGenerator.generateNumericOtp(6);
        String otpHash = tokenHashService.hash(otp);

        session.setOtpHash(otpHash);
        session.setRequests(session.getRequests() + 1);
        mfaOtpRepository.save(session);

        mailSenderPort.sendMfaOtp(user.getEmail(), otp);

        String uaHash = tokenHashService.hash(ua);
        String ipHash = tokenHashService.hash(ip);

        auditLogger.loginOtpResend(user.getId(), ip, ipHash, ua, uaHash);
    }
}