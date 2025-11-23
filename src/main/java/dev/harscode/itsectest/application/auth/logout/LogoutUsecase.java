package dev.harscode.itsectest.application.auth.logout;

import java.util.UUID;

public interface LogoutUsecase {
    void logout(UUID userId, UUID sessionId);
}
