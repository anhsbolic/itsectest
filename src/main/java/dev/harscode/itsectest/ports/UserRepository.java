package dev.harscode.itsectest.ports;

import dev.harscode.itsectest.domain.user.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);

    Optional<User> findById(UUID id);
}
