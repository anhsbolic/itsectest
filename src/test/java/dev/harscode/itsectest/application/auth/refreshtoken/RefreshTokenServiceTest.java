package dev.harscode.itsectest.application.auth.refreshtoken;

import dev.harscode.itsectest.domain.auth.UserSession;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.repository.UserProfileRepository;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserSessionRepository;
import dev.harscode.itsectest.security.jwt.JwtTokenService;
import dev.harscode.itsectest.security.token.TokenHashService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private JwtTokenService jwtTokenService;

    @InjectMocks
    private RefreshTokenService service;

    private User user;
    private UUID userId;
    private UserSession session;

    @BeforeEach
    void init() {
        userId = UUID.randomUUID();

        user = new User();
        user.setId(userId);
        user.setEmail("abc@example.com");
        user.setStatus("active");
        user.setEmailVerified(true);
        user.setRole("admin");

        session = new UserSession();
        session.setUserId(userId);
        session.setSessionId(UUID.randomUUID());
        session.setUserAgentHash("ua-hash");
        session.setIpAddressHash("ip-hash");
        session.setRefreshTokenHash("old-token-hash");
    }

    @Test
    void refresh_shouldThrow_whenMissingToken() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("   ", "UA", "IP");

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Missing refresh token");
    }

    @Test
    void refresh_shouldThrow_whenSessionNotFound() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("raw", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("raw"), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired session");
    }

    @Test
    void refresh_shouldRevokeSessionAndThrow_whenDeviceMismatch() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("rawtoken", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("rawtoken"), any()))
                .thenReturn(Optional.of(session));

        when(tokenHashService.hash("UA")).thenReturn("wrong-ua");
        when(tokenHashService.hash("IP")).thenReturn("wrong-ip");

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("bound to another device");

        verify(userSessionRepository).revokeSession(eq(session.getSessionId()), any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void refresh_shouldThrow_whenUserNotFound() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("rawtoken", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("rawtoken"), any()))
                .thenReturn(Optional.of(session));

        when(tokenHashService.hash("UA")).thenReturn("ua-hash");
        when(tokenHashService.hash("IP")).thenReturn("ip-hash");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refresh_shouldThrow_whenUserInactive() {
        user.setStatus("inactive");

        RefreshTokenCommand cmd = new RefreshTokenCommand("rawtoken", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("rawtoken"), any())).thenReturn(Optional.of(session));
        when(tokenHashService.hash("UA")).thenReturn("ua-hash");
        when(tokenHashService.hash("IP")).thenReturn("ip-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("User is not active");
    }

    @Test
    void refresh_shouldThrow_whenEmailNotVerified() {
        user.setEmailVerified(false);

        RefreshTokenCommand cmd = new RefreshTokenCommand("rawtoken", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("rawtoken"), any())).thenReturn(Optional.of(session));
        when(tokenHashService.hash("UA")).thenReturn("ua-hash");
        when(tokenHashService.hash("IP")).thenReturn("ip-hash");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.refresh(cmd))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("email not verified");
    }

    @Test
    void refresh_shouldRotateTokenAndReturnNewTokens() {
        RefreshTokenCommand cmd = new RefreshTokenCommand("rawtoken", "UA", "IP");

        when(userSessionRepository.findValidByRefreshTokenHash(eq("rawtoken"), any()))
                .thenReturn(Optional.of(session));

        when(tokenHashService.hash("UA")).thenReturn("ua-hash");
        when(tokenHashService.hash("IP")).thenReturn("ip-hash");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setFullName("Test User");
        when(userProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        when(tokenHashService.generateRefreshToken()).thenReturn("new-raw");
        when(tokenHashService.hash("new-raw")).thenReturn("new-hash");

        when(jwtTokenService.generateAccessToken(
                anyString(), anyString(), anyString(), anyBoolean()
        )).thenReturn("new-access-token");

        RefreshTokenResult result = service.refresh(cmd);

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.profile()).isEqualTo(profile);
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.newRefreshTokenRaw()).isEqualTo("new-raw");
        assertThat(session.getRefreshTokenHash()).isEqualTo("new-hash");
        assertThat(session.getExpiresAt()).isAfter(Instant.now());

        verify(userSessionRepository).save(session);
    }
}