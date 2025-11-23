package dev.harscode.itsectest.application.auth.logout;

import dev.harscode.itsectest.ports.UserSessionRepository;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class LogoutService implements LogoutUsecase {

    private final UserSessionRepository userSessionRepository;

    public LogoutService(UserSessionRepository userSessionRepository) {
        this.userSessionRepository = userSessionRepository;
    }

    @Override
    @Transactional
    public void logout(UUID userId, UUID sessionId) {
        if (sessionId == null) {
            throw new UnauthorizedException("Invalid session");
        }
        userSessionRepository.revokeSession(sessionId, Instant.now());
    }
}
