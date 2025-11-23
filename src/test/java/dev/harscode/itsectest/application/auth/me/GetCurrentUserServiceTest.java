package dev.harscode.itsectest.application.auth.me;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.web.exception.UnauthorizedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetCurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @InjectMocks
    private GetCurrentUserService service;

    private UUID userId;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
    }

    private User makeActiveVerifiedUser() {
        User u = new User();
        u.setId(userId);
        u.setUsername("john");
        u.setEmail("john@example.com");
        u.setRole("viewer");
        u.setStatus("active");
        u.setEmailVerified(true);
        u.setCreatedAt(Instant.now());
        u.setUpdatedAt(Instant.now());
        return u;
    }

    @Test
    void getCurrentUser_shouldReturnUserAndProfile_whenUserIsActiveAndVerified() {
        User user = makeActiveVerifiedUser();
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setFullName("John Doe");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        GetCurrentUserResult result = service.getCurrentUser(userId);

        assertThat(result.user().getId()).isEqualTo(userId);
        assertThat(result.profile().getFullName()).isEqualTo("John Doe");

        verify(userRepository).findById(userId);
        verify(userProfileRepository).findByUserId(userId);
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorized_whenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentUser(userId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verifyNoInteractions(userProfileRepository);
    }

    @Test
    void getCurrentUser_shouldThrowUnauthorized_whenUserInactiveOrUnverified() {
        User u = makeActiveVerifiedUser();
        u.setStatus("inactive");

        when(userRepository.findById(userId)).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> service.getCurrentUser(userId))
                .isInstanceOf(UnauthorizedException.class);

        verify(userRepository).findById(userId);
        verifyNoInteractions(userProfileRepository);
    }
}