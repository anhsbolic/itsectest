package dev.harscode.itsectest.application.article;

import dev.harscode.itsectest.domain.article.Article;
import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.repository.ArticleRepository;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ArticleService implements ArticleUsecase {

    private final ArticleRepository repo;
    private final AuditLogRepository auditLogRepository;
    private final TokenHashService tokenHashService;

    public ArticleService(
            ArticleRepository repo,
            AuditLogRepository auditLogRepository,
            TokenHashService tokenHashService
    ) {
        this.repo = repo;
        this.auditLogRepository = auditLogRepository;
        this.tokenHashService = tokenHashService;
    }

    @Override
    public ArticleResult create(CreateArticleCommand cmd) {
        Article a = new Article();
        a.setTitle(cmd.title().trim());
        a.setContent(cmd.content().trim());
        a.setAuthorId(cmd.authorId());
        a.setStatus("published");
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());

        Article saved = repo.save(a);
        return toResult(saved);
    }
    private AuditLog buildAuditLog(
            String activity,
            String description,
            boolean success,
            String userAgent,
            String ipAddress,
            String uaHash,
            String ipHash,
            UUID userId,
            String entityType,
            UUID entityId
    ) {
        AuditLog log = new AuditLog();
        log.setActivity(activity);
        log.setDescription(description);
        log.setSuccess(success);
        log.setUserAgent(userAgent);
        log.setUserAgentHash(uaHash);
        log.setIpAddress(ipAddress);
        log.setIpAddressHash(ipHash);
        log.setUserId(userId);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setActivityTime(Instant.now());
        return log;
    }


    @Override
    public ArticleResult update(UpdateArticleCommand cmd) {
        Article a = repo.findWithFilters(cmd.articleId(), cmd.authorId(), null).orElseThrow(() ->
                new NotFoundException("Article not found")
        );

        a.setTitle(cmd.title().trim());
        a.setContent(cmd.content().trim());
        a.setStatus(cmd.status().trim());
        a.setUpdatedAt(Instant.now());

        Article saved = repo.save(a);

        String userAgent = cmd.userAgent() == null ? "" : cmd.userAgent().trim();
        String uaHash = tokenHashService.hash(userAgent);
        String ipAddress = cmd.ipAddress() == null ? "" : cmd.ipAddress().trim();
        String ipHash = tokenHashService.hash(ipAddress);
        auditLogRepository.save(buildAuditLog(
                "ARTICLE_UPDATE",
                "Articel Updated By User",
                false,
                userAgent,
                ipAddress,
                uaHash,
                ipHash,
                cmd.userId(),
                "ARTICLE",
                null
        ));

        return toResult(saved);
    }

    @Override
    public void delete(DeleteArticleCommand cmd) {
        Article a = repo.findWithFilters(cmd.articleId(), cmd.authorId(), null).orElseThrow(() ->
                new NotFoundException("Article not found")
        );
        repo.softDelete(a.getId());
    }

    @Override
    public ArticleResult getById(UUID id, UUID authorId, String requiredStatus) {
        Article a = repo.findWithFilters(id, authorId, requiredStatus).orElseThrow(() ->
                new NotFoundException("Article not found")
        );
        return toResult(a);
    }

    @Override
    public PagedArticleResult list(ListArticlesQuery query) {
        int requestedPage = query.page() <= 0 ? 1 : query.page();
        int pageIndex = requestedPage - 1;
        int size = query.size() <= 0 ? 10 : Math.min(query.size(), 100);

        String search = (query.search() == null || query.search().isBlank()) ? null : query.search().trim();
        UUID authorId = query.authorId();
        String status = (query.status() == null || query.status().isBlank()) ? null : query.status().trim();

        PageRequest pageable = PageRequest.of(pageIndex, size);
        Page<Article> pageResult = repo.findAll(search, authorId, status, pageable);

        return new PagedArticleResult(
                pageResult.map(this::toResult).getContent(),
                requestedPage,
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
                a.getStatus(),
                a.getAuthorId(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}
