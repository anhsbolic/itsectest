package dev.harscode.itsectest.adapters.web.user.dto;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;

public record UserAdminResult(
        User user,
        UserProfile profile
) {
}
