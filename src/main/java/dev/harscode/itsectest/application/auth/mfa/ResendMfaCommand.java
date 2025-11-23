package dev.harscode.itsectest.application.auth.mfa;

public record ResendMfaCommand(
        String mfaSessionId,
        String userAgent,
        String ipAddress
) {
}
