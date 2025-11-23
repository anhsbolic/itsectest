package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;

public record RefreshTokenResult(
        User user,
        UserProfile profile,
        String accessToken,
        String newRefreshTokenRaw
) {
}
