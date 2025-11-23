package dev.harscode.itsectest.adapters.persistence.article;

import dev.harscode.itsectest.domain.article.Article;
import dev.harscode.itsectest.ports.repository.ArticleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class ArticleRepositoryJpaAdapter implements ArticleRepository {

    private final ArticleJpaRepository jpa;

    public ArticleRepositoryJpaAdapter(ArticleJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Article save(Article article) {
        ArticleEntity entity = toEntity(article);
        ArticleEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Article> findById(UUID id) {
        return jpa.findById(id)
                .filter(e -> e.getDeletedAt() == null)
                .map(this::toDomain);
    }

    @Override
    public Page<Article> findAll(String search, UUID authorId, Pageable pageable) {
        Page<ArticleEntity> page = jpa.search(search, authorId, pageable);
        return page.map(this::toDomain);
    }

    @Override
    public void softDelete(UUID id) {
        jpa.findById(id).ifPresent(e -> {
            if (e.getDeletedAt() == null) {
                e.setDeletedAt(Instant.now());
                jpa.save(e);
            }
        });
    }

    private ArticleEntity toEntity(Article a) {
        ArticleEntity e = new ArticleEntity();
        e.setId(a.getId());
        e.setTitle(a.getTitle());
        e.setContent(a.getContent());
        e.setAuthorId(a.getAuthorId());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        e.setDeletedAt(a.getDeletedAt());
        return e;
    }

    private Article toDomain(ArticleEntity e) {
        Article a = new Article();
        a.setId(e.getId());
        a.setTitle(e.getTitle());
        a.setContent(e.getContent());
        a.setAuthorId(e.getAuthorId());
        a.setCreatedAt(e.getCreatedAt());
        a.setUpdatedAt(e.getUpdatedAt());
        a.setDeletedAt(e.getDeletedAt());
        return a;
    }
}
