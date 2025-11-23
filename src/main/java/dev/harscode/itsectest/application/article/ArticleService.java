package dev.harscode.itsectest.application.article;

import dev.harscode.itsectest.domain.article.Article;
import dev.harscode.itsectest.ports.repository.ArticleRepository;
import dev.harscode.itsectest.web.exception.ForbiddenException;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ArticleService implements ArticleUsecase {

    private final ArticleRepository repo;

    public ArticleService(ArticleRepository repo) {
        this.repo = repo;
    }

    @Override
    public ArticleResult create(CreateArticleCommand cmd) {
        Article a = new Article();
        a.setTitle(cmd.title().trim());
        a.setContent(cmd.content().trim());
        a.setAuthorId(cmd.authorId());
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());

        Article saved = repo.save(a);
        return toResult(saved);
    }

    @Override
    public ArticleResult update(UpdateArticleCommand cmd) {
        Article a = repo.findById(cmd.articleId()).orElseThrow(() ->
                new NotFoundException("Article not found")
        );

        if (a.isDeleted()) {
            throw new NotFoundException("Article not found");
        }

        boolean isAuthor = a.getAuthorId().equals(cmd.currentUserId());
        boolean isSuperAdmin = "super_admin".equalsIgnoreCase(cmd.currentUserRole());
        if (!isAuthor && !isSuperAdmin) {
            throw new ForbiddenException("You are not allowed to modify this article");
        }

        a.setTitle(cmd.title().trim());
        a.setContent(cmd.content().trim());
        a.setUpdatedAt(Instant.now());

        Article saved = repo.save(a);
        return toResult(saved);
    }

    @Override
    public void delete(DeleteArticleCommand cmd) {
        Article a = repo.findById(cmd.articleId())
                .orElseThrow(() -> new NotFoundException("Article not found"));

        boolean isAuthor = a.getAuthorId().equals(cmd.currentUserId());
        boolean isSuperAdmin = "super_admin".equalsIgnoreCase(cmd.currentUserRole());
        if (!isAuthor && !isSuperAdmin) {
            throw new ForbiddenException("You are not allowed to delete this article");
        }

        repo.softDelete(cmd.articleId());
    }

    @Override
    public ArticleResult getById(UUID id) {
        Article a = repo.findById(id).orElseThrow(() ->
                new NotFoundException("Article not found")
        );
        return toResult(a);
    }

    @Override
    public PagedArticleResult list(ListArticlesQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() <= 0 ? 10 : Math.min(query.size(), 100);

        PageRequest pageable = PageRequest.of(page, size);
        Page<Article> pageResult = repo.findAll(
                query.search() == null ? "" : query.search().trim(),
                query.authorId(),
                pageable
        );

        return new PagedArticleResult(
                pageResult.map(this::toResult).getContent(),
                page,
                size,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    private ArticleResult toResult(Article a) {
        return new ArticleResult(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getAuthorId(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
