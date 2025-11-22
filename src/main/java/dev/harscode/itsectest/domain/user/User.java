package dev.harscode.itsectest.domain.user;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class User {
    private UUID id;
    private String username;
    private String email;
    private String passwordHash;
    private String status;
    private String role;
    private boolean emailVerified;
    private boolean mfaEnabled;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant updatedAt;
}