package dev.harscode.itsectest.application.article;

import java.util.UUID;

public record UpdateArticleCommand(
        UUID articleId,
        UUID currentUserId,
        String currentUserRole,
        String title,
        String content
) {
}
