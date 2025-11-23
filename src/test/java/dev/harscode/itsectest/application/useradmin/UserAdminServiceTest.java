package dev.harscode.itsectest.application.useradmin;

import dev.harscode.itsectest.adapters.web.user.dto.CreateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UpdateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UserAdminResult;
import dev.harscode.itsectest.application.auditlog.AuditLogger;
import dev.harscode.itsectest.application.metadata.RequestMetadata;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.crypt.PasswordHasher;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.security.token.TokenHashService;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UserAdminService service;

    private UUID userId;
    private UUID actorId;
    private RequestMetadata metadata;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        metadata = new RequestMetadata(
                "127.0.0.1",
                userId.toString(),
                "editor",
                actorId.toString()
        );
    }

    private User makeUser() {
        User u = new User();
        u.setId(userId);
        u.setUsername("admin");
        u.setEmail("admin@example.com");
        u.setRole("super_admin");
        u.setStatus("active");
        u.setEmailVerified(true);
        u.setMfaEnabled(true);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }

    private UserProfile makeProfile() {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setFullName("Admin User");
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return p;
    }

    @Test
    void list_shouldReturnUsersWithProfile() {
        User u1 = makeUser();
        User u2 = makeUser();
        u2.setId(UUID.randomUUID());
        u2.setUsername("editor");

        when(userRepository.findPage(0, 10)).thenReturn(List.of(u1, u2));
        when(userProfileRepository.findByUserId(u1.getId()))
                .thenReturn(Optional.of(makeProfile()));
        when(userProfileRepository.findByUserId(u2.getId()))
                .thenReturn(Optional.empty());

        List<UserAdminResult> results = service.list(0, 10);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).user().getUsername()).isEqualTo("admin");
        assertThat(results.get(1).user().getUsername()).isEqualTo("editor");

        verify(userRepository).findPage(0, 10);
        verify(userProfileRepository).findByUserId(u1.getId());
        verify(userProfileRepository).findByUserId(u2.getId());
    }

    @Test
    void get_shouldReturnUserAndProfile_whenFound() {
        User u = makeUser();
        UserProfile p = makeProfile();

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(p));

        UserAdminResult result = service.get(userId);

        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.profile().getFullName()).isEqualTo("Admin User");

        verify(userRepository).findById(userId);
        verify(userProfileRepository).findByUserId(userId);
    }

    @Test
    void get_shouldThrowNotFound_whenUserMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(userId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void create_shouldCreateUserAndProfileAndLogAudit() {
        CreateUserCommand cmd = new CreateUserCommand(
                "admin",
                "admin@example.com",
                "Secret123!",
                "Admin User",
                "super_admin"
        );

        when(passwordHasher.hash("Secret123!")).thenReturn("hashed-password");

        when(userRepository.create(any(User.class)))
                .thenAnswer(invocation -> {
                    User u = invocation.getArgument(0);
                    u.setId(userId);
                    return u;
                });

        when(tokenHashService.hash(metadata.ip())).thenReturn("ip-hash");
        when(tokenHashService.hash(metadata.userAgent())).thenReturn("ua-hash");

        UserAdminResult result = service.create(cmd, metadata);

        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.user().getUsername()).isEqualTo("admin");
        assertThat(result.user().getEmail()).isEqualTo("admin@example.com");
        assertThat(result.user().getRole()).isEqualTo("super_admin");
        assertThat(result.profile().getFullName()).isEqualTo("Admin User");

        verify(passwordHasher).hash("Secret123!");

        verify(userRepository).create(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));

        verify(auditLogger).log(
                eq(actorId),
                eq("USER_CREATE"),
                eq("User"),
                eq(userId),
                eq(true),
                eq(metadata.ip()),
                eq("ip-hash"),
                eq(metadata.userAgent()),
                eq("ua-hash"),
                contains("Created user:")
        );
    }

    @Test
    void update_shouldUpdateUserAndProfileAndLogAudit() {
        User existingUser = makeUser();
        UserProfile existingProfile = makeProfile();

        UpdateUserCommand cmd = new UpdateUserCommand(
                "new-email@example.com",
                "New Name",
                "editor",
                "inactive",
                false
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        when(userRepository.update(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(userProfileRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingProfile));

        when(tokenHashService.hash(metadata.ip())).thenReturn("ip-hash");
        when(tokenHashService.hash(metadata.userAgent())).thenReturn("ua-hash");

        UserAdminResult result = service.update(userId, cmd, metadata);

        assertThat(result.user().getEmail()).isEqualTo("new-email@example.com");
        assertThat(result.user().getRole()).isEqualTo("editor");
        assertThat(result.user().getStatus()).isEqualTo("inactive");
        assertThat(result.user().isMfaEnabled()).isFalse();
        assertThat(result.profile().getFullName()).isEqualTo("New Name");

        verify(userRepository).findById(userId);
        verify(userRepository).update(any(User.class));
        verify(userProfileRepository).findByUserId(userId);
        verify(userProfileRepository).save(any(UserProfile.class));

        verify(auditLogger).log(
                eq(actorId),
                eq("USER_UPDATE"),
                eq("User"),
                eq(userId),
                eq(true),
                eq(metadata.ip()),
                eq("ip-hash"),
                eq(metadata.userAgent()),
                eq("ua-hash"),
                contains("Updated user:")
        );
    }

    @Test
    void update_shouldThrowNotFound_whenUserMissing() {
        UpdateUserCommand cmd = new UpdateUserCommand(
                null, null, null, null, null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, cmd, metadata))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userProfileRepository, auditLogger);
    }

    @Test
    void delete_shouldSoftDeleteAndLogAudit() {
        User existingUser = makeUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        when(tokenHashService.hash(metadata.ip())).thenReturn("ip-hash");
        when(tokenHashService.hash(metadata.userAgent())).thenReturn("ua-hash");

        service.delete(userId, metadata);

        verify(userRepository).findById(userId);
        verify(userRepository).softDelete(userId);

        verify(auditLogger).log(
                eq(actorId),
                eq("USER_DELETE"),
                eq("User"),
                eq(userId),
                eq(true),
                eq(metadata.ip()),
                eq("ip-hash"),
                eq(metadata.userAgent()),
                eq("ua-hash"),
                contains("Deleted user:")
        );
    }

    @Test
    void delete_shouldThrowNotFound_whenUserMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(userId, metadata))
                .isInstanceOf(NotFoundException.class);

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(auditLogger);
    }
}
