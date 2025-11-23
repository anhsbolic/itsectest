package dev.harscode.itsectest.application.auth.mfa;

public interface ResendMfaUsecase {
    void resend(ResendMfaCommand command);
}
