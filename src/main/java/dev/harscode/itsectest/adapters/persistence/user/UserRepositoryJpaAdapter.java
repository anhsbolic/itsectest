package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.ports.crypt.PiiCrypto;
import dev.harscode.itsectest.ports.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final PiiCrypto piiCrypto;

    public UserRepositoryJpaAdapter(UserJpaRepository jpa, PiiCrypto piiCrypto) {
        this.jpa = jpa;
        this.piiCrypto = piiCrypto;
    }

    @Override
    public boolean existsByUsername(String username) {
        String normalized = username.toLowerCase();
        String hash = piiCrypto.hash(normalized);
        return jpa.existsByUsernameHash(hash);
    }

    @Override
    public boolean existsByEmail(String email) {
        String normalized = email.toLowerCase();
        String hash = piiCrypto.hash(normalized);
        return jpa.existsByEmailHash(hash);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        String norm = usernameOrEmail.trim().toLowerCase();
        String hash = piiCrypto.hash(norm);

        return jpa
                .findByUsernameHashOrEmailHash(hash, hash)
                .map(this::toDomain);
    }


    @Override
    public void markEmailVerifiedAndActivate(UUID userId) {
        jpa.findById(userId).ifPresent(entity -> {
            entity.setEmailVerified(true);
            if ("inactive".equalsIgnoreCase(entity.getStatus())) {
                entity.setStatus("active");
            }
            jpa.save(entity);
        });
    }

    @Override
    public User create(User user) {
        UserEntity e = toEntity(user);
        e.setCreatedAt(Instant.now());
        e.setUpdatedAt(Instant.now());
        return toDomain(jpa.save(e));
    }

    @Override
    public User update(User user) {
        UserEntity e = toEntity(user);
        e.setUpdatedAt(Instant.now());
        return toDomain(jpa.save(e));
    }

    @Override
    public List<User> findPage(int page, int size) {
        return jpa.findAllByDeletedAtIsNull(PageRequest.of(page, size))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void softDelete(UUID id) {
        jpa.softDelete(id, Instant.now());
    }

    private UserEntity toEntity(User user) {
        UserEntity e = new UserEntity();
        e.setId(user.getId());

        String usernameNorm = user.getUsername().toLowerCase();
        String emailNorm = user.getEmail().toLowerCase();

        e.setUsernameEnc(piiCrypto.encrypt(usernameNorm));
        e.setUsernameHash(piiCrypto.hash(usernameNorm));

        e.setEmailEnc(piiCrypto.encrypt(emailNorm));
        e.setEmailHash(piiCrypto.hash(emailNorm));

        e.setPasswordHash(user.getPasswordHash());
        e.setStatus(user.getStatus());
        e.setRole(user.getRole());
        e.setEmailVerified(user.isEmailVerified());
        e.setMfaEnabled(user.isMfaEnabled());
        e.setLastLoginAt(user.getLastLoginAt());
        e.setCreatedAt(user.getCreatedAt());
        e.setUpdatedAt(user.getUpdatedAt());

        return e;
    }

    private User toDomain(UserEntity e) {
        User u = new User();
        u.setId(e.getId());
        u.setUsername(piiCrypto.decrypt(e.getUsernameEnc()));
        u.setEmail(piiCrypto.decrypt(e.getEmailEnc()));
        u.setPasswordHash(e.getPasswordHash());
        u.setStatus(e.getStatus());
        u.setRole(e.getRole());
        u.setEmailVerified(e.isEmailVerified());
        u.setMfaEnabled(e.isMfaEnabled());
        u.setLastLoginAt(e.getLastLoginAt());
        u.setCreatedAt(e.getCreatedAt());
        u.setUpdatedAt(e.getUpdatedAt());
        return u;
    }
}