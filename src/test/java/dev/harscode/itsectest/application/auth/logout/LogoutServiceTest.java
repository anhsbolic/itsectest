package dev.harscode.itsectest.application.auth.logout;

import dev.harscode.itsectest.ports.repository.UserSessionRepository;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

    @Mock
    private UserSessionRepository userSessionRepository;

    @InjectMocks
    private LogoutService logoutService;

    @Test
    void logout_shouldRevokeSession_whenValidSessionIdProvided() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        logoutService.logout(userId, sessionId);

        verify(userSessionRepository, times(1))
                .revokeSession(eq(sessionId), any(Instant.class));
    }

    @Test
    void logout_shouldThrowUnauthorizedException_whenSessionIdIsNull() {
        UUID userId = UUID.randomUUID();

        assertThatThrownBy(() -> logoutService.logout(userId, null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid session");

        verifyNoInteractions(userSessionRepository);
    }
}