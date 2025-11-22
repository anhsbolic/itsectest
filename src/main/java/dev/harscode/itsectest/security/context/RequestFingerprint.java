package dev.harscode.itsectest.security.context;

public record RequestFingerprint(
        String ip,
        String userAgent
) {
}
