package dev.harscode.itsectest.application.auth.emailverification;

public interface EmailVerificationUsecase {
    void verify(String token);
}
