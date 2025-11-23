package dev.harscode.itsectest.adapters.persistence.article;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ArticleJpaRepository extends JpaRepository<ArticleEntity, UUID> {

    @Query("""
            SELECT a FROM ArticleEntity a
            WHERE a.deletedAt IS NULL
              AND (:authorId IS NULL OR a.authorId = :authorId)
              AND (
                    :search IS NULL
                    OR :search = ''
                    OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%'))
                    OR LOWER(a.content) LIKE LOWER(CONCAT('%', :search, '%'))
              )
            """)
    Page<ArticleEntity> search(
            @Param("search") String search,
            @Param("authorId") UUID authorId,
            Pageable pageable
    );
}
