package dev.harscode.itsectest.ports;

import dev.harscode.itsectest.domain.auth.UserSession;

public interface UserSessionRepository {
    UserSession create(UserSession session);
}
