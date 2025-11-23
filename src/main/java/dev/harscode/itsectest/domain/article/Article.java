package dev.harscode.itsectest.domain.article;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Article {
    private UUID id;
    private String title;
    private String content;
    private UUID authorId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
