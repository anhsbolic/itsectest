package dev.harscode.itsectest.application.auth;

public interface RefreshTokenUsecase {
    RefreshTokenResult refresh(RefreshTokenCommand command);
}
