package dev.harscode.itsectest.domain.user;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class UserProfile {
    private UUID userId;
    private String fullName;
    private String phone;
    private String avatarUrl;
    private Instant createdAt;
    private Instant updatedAt;
}