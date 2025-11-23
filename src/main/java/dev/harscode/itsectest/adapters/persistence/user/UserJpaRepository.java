package dev.harscode.itsectest.adapters.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsernameHash(String usernameHash);

    boolean existsByEmailHash(String emailHash);

    Optional<UserEntity> findByUsernameHashOrEmailHash(String usernameHash, String emailHash);
}