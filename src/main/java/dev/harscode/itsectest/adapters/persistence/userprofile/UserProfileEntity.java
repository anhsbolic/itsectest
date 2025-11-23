package dev.harscode.itsectest.adapters.persistence.userprofile;


import dev.harscode.itsectest.adapters.persistence.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "user_profiles")
public class UserProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "full_name_enc", nullable = false)
    private byte[] fullNameEnc;

    @Column(name = "full_name_hash", nullable = false)
    private String fullNameHash;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "phone_enc")
    private byte[] phoneEnc;

    @Column(name = "phone_hash")
    private String phoneHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}