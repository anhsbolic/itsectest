package dev.harscode.itsectest.adapters.persistence.user;

import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.PiiCrypto;
import dev.harscode.itsectest.ports.UserProfileRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class UserProfileRepositoryJpaAdapter implements UserProfileRepository {

    private final UserProfileJpaRepository jpaRepository;
    private final UserJpaRepository userJpaRepository;   // tambahkan

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
    public UserProfile save(UserProfile profile) {
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

        return profile;
    }
}