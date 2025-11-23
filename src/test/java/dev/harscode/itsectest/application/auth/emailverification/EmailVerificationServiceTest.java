package dev.harscode.itsectest.application.auth.emailverification;

import dev.harscode.itsectest.domain.auth.UserToken;
import dev.harscode.itsectest.ports.repository.UserRepository;
import dev.harscode.itsectest.ports.repository.UserTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserTokenRepository userTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EmailVerificationService service;

    private UUID userId;
    private UUID tokenId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        tokenId = UUID.randomUUID();
    }

    private UserToken makeToken() {
        UserToken t = new UserToken();
        t.setId(tokenId);
        t.setUserId(userId);
        t.setTokenHash("abc123");
        t.setTokenType("email-verification");
        t.setExpiresAt(Instant.now().plusSeconds(3600));
        return t;
    }

    @Test
    void verify_shouldMarkTokenUsed_andActivateUser_whenValid() {
        UserToken token = makeToken();

        when(userTokenRepository.findValidToken("abc123", "email-verification"))
                .thenReturn(Optional.of(token));

        service.verify("abc123");

        verify(userTokenRepository).findValidToken("abc123", "email-verification");
        verify(userTokenRepository).markUsed(tokenId);
        verify(userRepository).markEmailVerifiedAndActivate(userId);
    }

    @Test
    void verify_shouldThrowException_whenTokenInvalid() {
        when(userTokenRepository.findValidToken("invalid", "email-verification"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verify("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired token");

        verify(userTokenRepository).findValidToken("invalid", "email-verification");
        verifyNoMoreInteractions(userTokenRepository);
        verifyNoInteractions(userRepository);
    }
}