package dev.harscode.itsectest.domain.user;

import lombok.Data;

import java.util.UUID;

@Data
public class AuthUser {
    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String role;
}