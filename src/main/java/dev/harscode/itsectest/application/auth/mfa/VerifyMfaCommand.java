package dev.harscode.itsectest.application.auth.mfa;

public record VerifyMfaCommand(
        String mfaSessionId,
        String otp,
        String userAgent,
        String ipAddress
) {
}
