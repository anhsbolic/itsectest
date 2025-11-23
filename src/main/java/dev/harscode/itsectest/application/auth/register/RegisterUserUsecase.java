package dev.harscode.itsectest.application.auth.register;

public interface RegisterUserUsecase {
    RegisterUserResult register(RegisterUserCommand command);
}