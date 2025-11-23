package dev.harscode.itsectest.application.article;

import java.util.UUID;

public interface ArticleUsecase {
    ArticleResult getById(UUID id, UUID authorId, String requiredStatus);

    PagedArticleResult list(ListArticlesQuery query);

    ArticleResult create(CreateArticleCommand cmd);

    ArticleResult update(UpdateArticleCommand cmd);

    void delete(DeleteArticleCommand cmd);
}
