package dev.harscode.itsectest.application.auth;

public interface LoginUserUsecase {
    LoginUserResult login(LoginUserCommand command);
}
