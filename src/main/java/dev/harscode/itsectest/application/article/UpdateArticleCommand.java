package dev.harscode.itsectest.application.article;

import java.util.UUID;

public record UpdateArticleCommand(
        UUID articleId,
        UUID authorId,
        String title,
        String content,
        String status,
        UUID userId,
        String userAgent,
        String ipAddress
) {
}
