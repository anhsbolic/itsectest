package dev.harscode.itsectest.adapters.web.auth;

import dev.harscode.itsectest.domain.user.AuthUser;

public record MeResponse(AuthUser user) {
}
