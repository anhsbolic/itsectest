package dev.harscode.itsectest.adapters.web.article.dto;


import java.time.Instant;
import java.util.UUID;

public record ArticleResponse(
        UUID id,
        String title,
        String content,
        UUID authorId,
        Instant createdAt,
        Instant updatedAt
) {
}
