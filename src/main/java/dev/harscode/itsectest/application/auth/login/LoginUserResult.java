package dev.harscode.itsectest.application.auth.login;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;

public record LoginUserResult(
        User user,
        UserProfile profile,
        String accessToken,
        String refreshTokenRaw
) {
}
