package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;

public record GetCurrentUserResult(User user, UserProfile profile) {
}
