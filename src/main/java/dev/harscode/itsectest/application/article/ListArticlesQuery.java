package dev.harscode.itsectest.application.article;

import java.util.UUID;

public record ListArticlesQuery(
        int page,
        int size,
        String search,
        UUID authorId
) {
}
