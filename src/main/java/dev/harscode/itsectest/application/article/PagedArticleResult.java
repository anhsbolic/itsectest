package dev.harscode.itsectest.application.article;

import java.util.List;

public record PagedArticleResult(
        List<ArticleResult> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}