package dev.harscode.itsectest.application.auth.login;

public interface LoginUserUsecase {
    LoginUserResult login(LoginUserCommand command);
}
