package dev.harscode.itsectest.application.auth.me;

import java.util.UUID;

public interface GetCurrentUserUsecase {
    GetCurrentUserResult getCurrentUser(UUID userId);
}
