package dev.harscode.itsectest.application.article;

import java.util.UUID;

public record CreateArticleCommand(
        UUID authorId,
        String title,
        String content
) {
}
