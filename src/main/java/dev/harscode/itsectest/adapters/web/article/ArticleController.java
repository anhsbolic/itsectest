package dev.harscode.itsectest.adapters.web.article;

import dev.harscode.itsectest.adapters.web.article.dto.ArticleListResponse;
import dev.harscode.itsectest.adapters.web.article.dto.ArticleResponse;
import dev.harscode.itsectest.adapters.web.article.dto.CreateArticleRequest;
import dev.harscode.itsectest.adapters.web.article.dto.UpdateArticleRequest;
import dev.harscode.itsectest.application.article.*;
import dev.harscode.itsectest.domain.user.AuthUser;
import dev.harscode.itsectest.security.permissions.article.ArticleAction;
import dev.harscode.itsectest.security.permissions.article.ArticlePermission;
import dev.harscode.itsectest.security.permissions.article.ArticleScope;
import dev.harscode.itsectest.web.dto.ApiResponse;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/articles")
@Tag(name = "Articles", description = "CRUD for articles")
public class ArticleController {

    private final ArticleUsecase articleUsecase;
    private final ArticlePermission permission;

    public ArticleController(ArticleUsecase articleUsecase, ArticlePermission permission) {
        this.articleUsecase = articleUsecase;
        this.permission = permission;
    }

    private UUID getCurrentUserId(HttpServletRequest request) {
        Object rawUserId = request.getAttribute("auth.userId");
        if (rawUserId instanceof UUID uuid) {
            return uuid;
        }
        if (rawUserId instanceof String s) {
            return UUID.fromString(s);
        }
        throw new UnauthorizedException("Missing authenticated user");
    }

    private String getCurrentUserRole(HttpServletRequest request) {
        Object rawRole = request.getAttribute("auth.role");
        if (rawRole == null) {
            throw new UnauthorizedException("Missing authenticated user");
        }

        return rawRole.toString();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ArticleResponse>> create(
            @AuthenticationPrincipal AuthUser user,
            @Valid @RequestBody CreateArticleRequest body,
            HttpServletRequest request
    ) {
        permission.resolveScope(user, ArticleAction.CREATE);

        UUID userId = getCurrentUserId(request);
        CreateArticleCommand cmd = new CreateArticleCommand(
                userId,
                body.title(),
                body.content()
        );

        ArticleResult result = articleUsecase.create(cmd);
        ArticleResponse response = toResponse(result);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("article created successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> getById(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable("id") UUID id,
            HttpServletRequest request
    ) {
        UUID userId = getCurrentUserId(request);

        ArticleScope scope = permission.resolveScope(user, ArticleAction.GET);
        UUID authorId = null;
        String status = null;
        switch (scope) {
            case OWN_ONLY -> authorId = userId;
            case PUBLIC_ONLY -> status = "published";
        }

        ArticleResult result = articleUsecase.getById(id, authorId, status);
        return ResponseEntity.ok(ApiResponse.ok("detail of article", toResponse(result)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ArticleListResponse>> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "authorId", required = false) UUID authorId,
            HttpServletRequest request
    ) {
        UUID userId = getCurrentUserId(request);

        ArticleScope scope = permission.resolveScope(user, ArticleAction.LIST);
        switch (scope) {
            case OWN_ONLY -> authorId = userId;
            case PUBLIC_ONLY -> status = "published";
        }

        ListArticlesQuery query = new ListArticlesQuery(page, size, search, status, authorId);
        PagedArticleResult result = articleUsecase.list(query);

        ArticleListResponse response = new ArticleListResponse(
                result.items().stream().map(this::toResponse).collect(Collectors.toList()),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );

        return ResponseEntity.ok(ApiResponse.ok("list of articles", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> update(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateArticleRequest body,
            HttpServletRequest request
    ) {
        UUID userId = getCurrentUserId(request);

        ArticleScope scope = permission.resolveScope(user, ArticleAction.UPDATE);
        UUID authorId = null;
        if (Objects.requireNonNull(scope) == ArticleScope.OWN_ONLY) {
            authorId = userId;
        }

        UpdateArticleCommand cmd = new UpdateArticleCommand(
                id,
                authorId,
                body.title(),
                body.content()
        );

        ArticleResult result = articleUsecase.update(cmd);
        return ResponseEntity.ok(ApiResponse.ok("article updated successfully", toResponse(result)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal AuthUser user,
            @PathVariable("id") UUID id,
            HttpServletRequest request
    ) {
        UUID userId = getCurrentUserId(request);

        ArticleScope scope = permission.resolveScope(user, ArticleAction.DELETE);
        UUID authorId = null;
        if (Objects.requireNonNull(scope) == ArticleScope.OWN_ONLY) {
            authorId = userId;
        }
        DeleteArticleCommand cmd = new DeleteArticleCommand(id, authorId);
        articleUsecase.delete(cmd);

        return ResponseEntity.ok(ApiResponse.ok("article deleted", null));
    }

    private ArticleResponse toResponse(ArticleResult r) {
        return new ArticleResponse(
                r.id(),
                r.title(),
                r.content(),
                r.authorId(),
                r.createdAt(),
                r.updatedAt()
        );
    }
}
