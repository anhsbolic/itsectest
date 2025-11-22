package dev.harscode.itsectest.application.auth;

public interface EmailVerificationUsecase {
    void verify(String token);
}
