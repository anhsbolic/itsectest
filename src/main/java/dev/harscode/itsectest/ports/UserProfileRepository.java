package dev.harscode.itsectest.ports;

import dev.harscode.itsectest.domain.user.UserProfile;

public interface UserProfileRepository {

    UserProfile save(UserProfile profile);
}
