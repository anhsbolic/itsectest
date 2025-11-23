package dev.harscode.itsectest.application.auth.mfa;

import dev.harscode.itsectest.application.auditlog.AuditLogger;
import dev.harscode.itsectest.application.auth.login.LoginUserResult;
import dev.harscode.itsectest.domain.auth.MfaOtpSession;
import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.mail.MailSenderPort;
import dev.harscode.itsectest.ports.repository.MfaOtpRepository;
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
public class MfaOtpService {
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
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenService jwtTokenService;
    private final TokenHashService hashService;
    private final AuditLogger auditLogger;

    public MfaOtpService(
            MfaOtpRepository mfaOtpRepository,
            TokenHashService tokenHashService,
            OtpGenerator otpGenerator,
            OtpRateLimiter otpRateLimiter,
            MailSenderPort mailSenderPort,
            UserSessionRepository userSessionRepository,
            JwtTokenService jwtTokenService,
            TokenHashService hashService,
            AuditLogger auditLogger
    ) {
        this.mfaOtpRepository = mfaOtpRepository;
        this.tokenHashService = tokenHashService;
        this.otpGenerator = otpGenerator;
        this.otpRateLimiter = otpRateLimiter;
        this.mailSenderPort = mailSenderPort;
        this.userSessionRepository = userSessionRepository;
        this.jwtTokenService = jwtTokenService;
        this.hashService = hashService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public String startMfaForLogin(User user, String ua, String ip) {
        // Global limit
        long globalCount = otpRateLimiter.incrementUserOtpRequests(user.getId(), GLOBAL_WINDOW);
        if (globalCount > GLOBAL_MAX_REQUESTS) {
            throw new TooManyOtpRequestsException("Too many MFA OTP requests, please try again later");
        }

        String otp = otpGenerator.generateNumericOtp(6);
        String otpHash = tokenHashService.hash(otp);

        String uaHash = hashService.hash(ua == null ? "" : ua);
        String ipHash = hashService.hash(ip == null ? "" : ip);

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

        // kirim email OTP
        mailSenderPort.sendMfaOtp(user.getEmail(), otp);

        // audit
        auditLogger.log(
                user.getId(),
                "MFA_OTP_SENT",
                "user",
                user.getId(),
                true,
                ip,
                ipHash,
                ua,
                uaHash,
                "MFA OTP sent for login"
        );

        return saved.getId();
    }


    @Transactional
    public void resendOtp(String mfaSessionId, User user, String ua, String ip) {
        MfaOtpSession session = mfaOtpRepository.findById(mfaSessionId)
                .orElseThrow(() -> new AuthenticationException("MFA_SESSION_NOT_FOUND", "MFA session not found"));

        if (session.isExpired()) {
            mfaOtpRepository.delete(mfaSessionId);
            throw new AuthenticationException("MFA_SESSION_EXPIRED", "MFA session expired");
        }

        if (!session.getUserId().equals(user.getId())) {
            throw new AuthenticationException("MFA_SESSION_USER_MISMATCH", "MFA session does not belong to this user");
        }

        if (!session.canRequestMore()) {
            throw new TooManyOtpRequestsException("Maximum OTP resend reached for this session");
        }

        // Global limit
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

        String uaHash = hashService.hash(ua == null ? "" : ua);
        String ipHash = hashService.hash(ip == null ? "" : ip);

        auditLogger.log(
                user.getId(),
                "MFA_OTP_RESEND",
                "user",
                user.getId(),
                true,
                ip,
                ipHash,
                ua,
                uaHash,
                "MFA OTP resent"
        );
    }


    @Transactional
    public LoginUserResult verifyOtp(
            String mfaSessionId,
            String otp,
            User user,
            UserProfile profile,
            String ua,
            String ip
    ) {

        MfaOtpSession session = mfaOtpRepository.findById(mfaSessionId)
                .orElseThrow(() -> new AuthenticationException("MFA_SESSION_NOT_FOUND", "MFA session not found"));

        if (session.isExpired()) {
            mfaOtpRepository.delete(mfaSessionId);
            throw new AuthenticationException("MFA_SESSION_EXPIRED", "MFA session expired");
        }

        if (!session.getUserId().equals(user.getId())) {
            throw new AuthenticationException("MFA_SESSION_USER_MISMATCH", "MFA session does not belong to this user");
        }

        if (!session.canAttemptMore()) {
            mfaOtpRepository.delete(mfaSessionId);
            throw new TooManyOtpAttemptsException("Too many invalid OTP attempts");
        }

        String otpHash = tokenHashService.hash(otp);
        boolean ok = otpHash.equals(session.getOtpHash());

        String uaHash = hashService.hash(ua == null ? "" : ua);
        String ipHash = hashService.hash(ip == null ? "" : ip);

        if (!ok) {
            session.setAttempts(session.getAttempts() + 1);
            mfaOtpRepository.save(session);

            auditLogger.log(
                    user.getId(),
                    "MFA_OTP_VERIFY",
                    "user",
                    user.getId(),
                    false,
                    ip,
                    ipHash,
                    ua,
                    uaHash,
                    "Invalid MFA OTP"
            );

            throw new AuthenticationException("MFA_OTP_INVALID", "Invalid OTP");
        }

        // OTP valid – delete session
        mfaOtpRepository.delete(mfaSessionId);

        // create persistent session & JW
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
                true
        );

        auditLogger.log(
                user.getId(),
                "MFA_OTP_VERIFY",
                "user",
                user.getId(),
                true,
                ip,
                ipHash,
                ua,
                uaHash,
                "MFA verified and session created"
        );

        return new LoginUserResult(user, profile, accessToken, refreshTokenRaw);
    }
}
