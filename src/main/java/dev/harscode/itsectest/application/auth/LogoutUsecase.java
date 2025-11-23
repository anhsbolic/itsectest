package dev.harscode.itsectest.application.auth;

import java.util.UUID;

public interface LogoutUsecase {
    void logout(UUID userId, UUID sessionId);
}
