package dev.harscode.itsectest.application.auth.mfa;

import dev.harscode.itsectest.application.auth.login.LoginUserResult;

public interface VerifyMfaUsecase {
    LoginUserResult verify(VerifyMfaCommand command);
}
