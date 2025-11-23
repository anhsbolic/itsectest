package dev.harscode.itsectest.adapters.web.user.dto;

public record UpdateUserCommand(
        String email,
        String fullName,
        String role,
        String status,
        Boolean mfaEnabled
) {
}