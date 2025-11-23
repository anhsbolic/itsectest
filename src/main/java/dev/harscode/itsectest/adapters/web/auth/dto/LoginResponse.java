package dev.harscode.itsectest.adapters.web.auth.dto;

import dev.harscode.itsectest.domain.user.AuthUser;

public record LoginResponse(
        String accessToken,
        AuthUser user
) {
}
