package dev.harscode.itsectest.application.metadata;

public record RequestMetadata(
        String ip,
        String userAgent,
        String actorRole,
        String actorId
) {
}
