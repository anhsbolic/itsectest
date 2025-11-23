package dev.harscode.itsectest.adapters.web.article.dto;

import java.util.List;

public record ArticleListResponse(
        List<ArticleResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
