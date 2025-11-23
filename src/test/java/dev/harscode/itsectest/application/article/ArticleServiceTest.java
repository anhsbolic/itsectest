package dev.harscode.itsectest.application.article;

import dev.harscode.itsectest.domain.article.Article;
import dev.harscode.itsectest.ports.repository.ArticleRepository;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository repo;

    @InjectMocks
    private ArticleService service;

    private UUID articleId;
    private UUID authorId;

    @BeforeEach
    void init() {
        articleId = UUID.randomUUID();
        authorId = UUID.randomUUID();
    }

    private Article makeArticle() {
        Article a = new Article();
        a.setId(articleId);
        a.setTitle("Title");
        a.setContent("Content");
        a.setAuthorId(authorId);
        a.setStatus("published");
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        return a;
    }

    // -----------------------------------------------------------------------
    // CREATE
    // -----------------------------------------------------------------------

    @Test
    void create_shouldSaveAndReturnResult() {
        CreateArticleCommand cmd = new CreateArticleCommand(
                authorId,
                " Hello ",
                " Content "
        );

        when(repo.save(any(Article.class)))
                .thenAnswer(i -> {
                    Article a = i.getArgument(0);
                    a.setId(articleId);
                    return a;
                });

        ArticleResult result = service.create(cmd);

        assertThat(result.id()).isEqualTo(articleId);
        assertThat(result.title()).isEqualTo("Hello");
        assertThat(result.content()).isEqualTo("Content");
        assertThat(result.authorId()).isEqualTo(authorId);

        verify(repo).save(any(Article.class));
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    @Test
    void update_shouldUpdateExistingAndSave() {
        Article existing = makeArticle();

        UpdateArticleCommand cmd = new UpdateArticleCommand(
                articleId,
                authorId,
                "New Title",
                "New Content",
                "draft"
        );

        when(repo.findWithFilters(articleId, authorId, null))
                .thenReturn(Optional.of(existing));

        when(repo.save(any(Article.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArticleResult result = service.update(cmd);

        assertThat(result.title()).isEqualTo("New Title");
        assertThat(result.content()).isEqualTo("New Content");
        assertThat(result.status()).isEqualTo("draft");

        verify(repo).findWithFilters(articleId, authorId, null);
        verify(repo).save(any(Article.class));
    }

    @Test
    void update_shouldThrowNotFound_whenMissing() {
        UpdateArticleCommand cmd = new UpdateArticleCommand(
                articleId, authorId, "A", "B", "draft"
        );

        when(repo.findWithFilters(articleId, authorId, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(cmd))
                .isInstanceOf(NotFoundException.class);

        verify(repo).findWithFilters(articleId, authorId, null);
        verifyNoMoreInteractions(repo);
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    @Test
    void delete_shouldSoftDelete() {
        Article existing = makeArticle();

        DeleteArticleCommand cmd = new DeleteArticleCommand(articleId, authorId);

        when(repo.findWithFilters(articleId, authorId, null))
                .thenReturn(Optional.of(existing));

        service.delete(cmd);

        verify(repo).findWithFilters(articleId, authorId, null);
        verify(repo).softDelete(articleId);
    }

    @Test
    void delete_shouldThrowNotFound_whenMissing() {
        DeleteArticleCommand cmd = new DeleteArticleCommand(articleId, authorId);

        when(repo.findWithFilters(articleId, authorId, null))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(cmd))
                .isInstanceOf(NotFoundException.class);

        verify(repo).findWithFilters(articleId, authorId, null);
    }

    // -----------------------------------------------------------------------
    // GET
    // -----------------------------------------------------------------------

    @Test
    void getById_shouldReturnArticle_whenFound() {
        Article existing = makeArticle();

        when(repo.findWithFilters(articleId, authorId, "published"))
                .thenReturn(Optional.of(existing));

        ArticleResult result = service.getById(articleId, authorId, "published");

        assertThat(result.id()).isEqualTo(articleId);
        assertThat(result.status()).isEqualTo("published");

        verify(repo).findWithFilters(articleId, authorId, "published");
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(repo.findWithFilters(articleId, authorId, "published"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(articleId, authorId, "published"))
                .isInstanceOf(NotFoundException.class);

        verify(repo).findWithFilters(articleId, authorId, "published");
    }

    // -----------------------------------------------------------------------
    // LIST
    // -----------------------------------------------------------------------

    @Test
    void list_shouldReturnPagedResult() {
        Article a1 = makeArticle();
        Article a2 = makeArticle();
        a2.setId(UUID.randomUUID());
        a2.setTitle("Another");

        Page<Article> page = new PageImpl<>(List.of(a1, a2), PageRequest.of(0, 10), 2);

        when(repo.findAll(null, authorId, null, PageRequest.of(0, 10)))
                .thenReturn(page);

        ListArticlesQuery query = new ListArticlesQuery(1, 10, null, null, authorId);

        PagedArticleResult result = service.list(query);

        assertThat(result.items()).hasSize(2);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.totalElements()).isEqualTo(2);

        verify(repo).findAll(null, authorId, null, PageRequest.of(0, 10));
    }
}