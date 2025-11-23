package dev.harscode.itsectest.application.article;

import java.time.Instant;
import java.util.UUID;

public record ArticleResult(
        UUID id,
        String title,
        String content,
        UUID authorId,
        Instant createdAt,
        Instant updatedAt
) {
}
