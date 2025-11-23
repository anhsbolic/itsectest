package dev.harscode.itsectest.application.auth.login;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;

public record LoginUserResult(
        boolean mfaRequired,
        String mfaSessionId,
        User user,
        UserProfile profile,
        String accessToken,
        String refreshTokenRaw
) {
    public static LoginUserResult mfaRequired(String mfaSessionId) {
        return new LoginUserResult(true, mfaSessionId, null, null, null, null);
    }

    public static LoginUserResult success(User user,
                                          UserProfile profile,
                                          String accessToken,
                                          String refreshTokenRaw) {
        return new LoginUserResult(false, null, user, profile, accessToken, refreshTokenRaw);
    }
}
