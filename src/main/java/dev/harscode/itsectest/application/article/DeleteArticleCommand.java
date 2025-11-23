package dev.harscode.itsectest.application.article;

import java.util.UUID;

public record DeleteArticleCommand(
        UUID articleId,
        UUID currentUserId,
        String currentUserRole
) {
}
