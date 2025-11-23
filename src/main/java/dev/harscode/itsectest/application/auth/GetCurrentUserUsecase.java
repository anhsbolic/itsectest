package dev.harscode.itsectest.application.auth;

import java.util.UUID;

public interface GetCurrentUserUsecase {
    GetCurrentUserResult getCurrentUser(UUID userId);
}
