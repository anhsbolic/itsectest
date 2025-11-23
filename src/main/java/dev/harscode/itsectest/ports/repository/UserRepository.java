package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.user.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    User save(User user);

    Optional<User> findById(UUID id);

    void markEmailVerifiedAndActivate(UUID userId);

    Optional<User> findByUsernameOrEmail(String usernameOrEmail);

    User create(User user);

    User update(User user);

    List<User> findPage(int page, int size);

    void softDelete(UUID id);
}
