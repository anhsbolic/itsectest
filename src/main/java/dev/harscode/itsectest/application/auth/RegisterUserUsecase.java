package dev.harscode.itsectest.application.auth;

public interface RegisterUserUsecase {
    RegisterUserResult register(RegisterUserCommand command);
}