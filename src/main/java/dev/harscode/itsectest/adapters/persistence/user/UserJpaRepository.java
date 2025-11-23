package dev.harscode.itsectest.adapters.persistence.user;

import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsernameHash(String usernameHash);

    boolean existsByEmailHash(String emailHash);

    Optional<UserEntity> findByUsernameHashOrEmailHash(String usernameHash, String emailHash);

    Optional<UserEntity> findByIdAndDeletedAtIsNull(UUID id);

    Page<UserEntity> findAllByDeletedAtIsNull(Pageable pageable);

    @Modifying
    @Query("UPDATE UserEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    void softDelete(@Param("id") UUID id, @Param("deletedAt") Instant deletedAt);

}