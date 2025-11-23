package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.user.UserProfile;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository {
    Optional<UserProfile> findByUserId(UUID userId);

    void save(UserProfile profile);
}
