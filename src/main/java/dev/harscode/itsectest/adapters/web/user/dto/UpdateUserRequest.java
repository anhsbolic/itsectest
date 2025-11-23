package dev.harscode.itsectest.adapters.web.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email @Size(max = 255) String email,
        @Size(max = 255) String fullName,
        String role,
        String status,
        Boolean mfaEnabled
) {
}
