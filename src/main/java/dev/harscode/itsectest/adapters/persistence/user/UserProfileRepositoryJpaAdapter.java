package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.PiiCrypto;
import dev.harscode.itsectest.ports.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserProfileRepositoryJpaAdapter implements UserProfileRepository {

    private final UserProfileJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;

    private final PiiCrypto piiCrypto;

    public UserProfileRepositoryJpaAdapter(
            UserProfileJpaRepository jpaRepository,
            UserJpaRepository userJpaRepository,
            PiiCrypto piiCrypto
    ) {
        this.jpaRepository = jpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.piiCrypto = piiCrypto;
    }

    @Override
    public Optional<UserProfile> findByUserId(UUID userId) {
        return jpaRepository.findById(userId).map(this::toDomain);
    }

    @Override
    public void save(UserProfile profile) {
        UserEntity userEntity = userJpaRepository.getReferenceById(profile.getUserId());

        UserProfileEntity entity = new UserProfileEntity();
        entity.setUser(userEntity);
        entity.setFullNameEnc(piiCrypto.encrypt(profile.getFullName()));
        entity.setFullNameHash(piiCrypto.hash(profile.getFullName()));

        entity.setAvatarUrl(profile.getAvatarUrl());

        if (profile.getPhone() != null) {
            entity.setPhoneEnc(piiCrypto.encrypt(profile.getPhone()));
            entity.setPhoneHash(piiCrypto.hash(profile.getPhone()));
        }

        Instant now = profile.getCreatedAt() != null ? profile.getCreatedAt() : Instant.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(profile.getUpdatedAt() != null ? profile.getUpdatedAt() : now);

        jpaRepository.save(entity);

    }

    private UserProfile toDomain(UserProfileEntity e) {
        UserProfile p = new UserProfile();
        p.setUserId(e.getUserId());
        p.setFullName(piiCrypto.decrypt(e.getFullNameEnc()));
        if (e.getPhoneEnc() != null) {
            p.setPhone(piiCrypto.decrypt(e.getPhoneEnc()));
        }
        p.setAvatarUrl(e.getAvatarUrl());
        p.setCreatedAt(e.getCreatedAt());
        p.setUpdatedAt(e.getUpdatedAt());
        return p;
    }
}