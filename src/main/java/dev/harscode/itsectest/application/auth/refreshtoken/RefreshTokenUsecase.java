package dev.harscode.itsectest.application.auth.refreshtoken;

public interface RefreshTokenUsecase {
    RefreshTokenResult refresh(RefreshTokenCommand command);
}
