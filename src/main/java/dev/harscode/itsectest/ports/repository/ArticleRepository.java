package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.article.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;


public interface ArticleRepository {

    Article save(Article article);

    Optional<Article> findWithFilters(UUID id, UUID authorId, String status);

    Page<Article> findAll(String search, UUID authorId, String status, Pageable pageable);

    void softDelete(UUID id);
}
