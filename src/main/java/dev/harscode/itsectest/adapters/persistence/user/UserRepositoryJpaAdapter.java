package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.ports.PiiCrypto;
import dev.harscode.itsectest.ports.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryJpaAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final PiiCrypto piiCrypto;

    public UserRepositoryJpaAdapter(UserJpaRepository jpaRepository,
                                    PiiCrypto piiCrypto
    ) {
        this.jpaRepository = jpaRepository;
        this.piiCrypto = piiCrypto;
    }

    @Override
    public boolean existsByUsername(String username) {
        String normalized = username.toLowerCase();
        String hash = piiCrypto.hash(normalized);
        return jpaRepository.existsByUsernameHash(hash);
    }

    @Override
    public boolean existsByEmail(String email) {
        String normalized = email.toLowerCase();
        String hash = piiCrypto.hash(normalized);
        return jpaRepository.existsByEmailHash(hash);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        String norm = usernameOrEmail.trim().toLowerCase();
        String hash = piiCrypto.hash(norm);

        return jpaRepository
                .findByUsernameHashOrEmailHash(hash, hash)
                .map(this::toDomain);
    }


    @Override
    public void markEmailVerifiedAndActivate(UUID userId) {
        jpaRepository.findById(userId).ifPresent(entity -> {
            entity.setEmailVerified(true);
            if ("inactive".equalsIgnoreCase(entity.getStatus())) {
                entity.setStatus("active");
            }
            jpaRepository.save(entity);
        });
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