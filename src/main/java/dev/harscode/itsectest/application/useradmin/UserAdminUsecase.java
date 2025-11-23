package dev.harscode.itsectest.application.useradmin;

import dev.harscode.itsectest.adapters.web.user.dto.CreateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UpdateUserCommand;
import dev.harscode.itsectest.adapters.web.user.dto.UserAdminResult;
import dev.harscode.itsectest.application.metadata.RequestMetadata;

import java.util.List;
import java.util.UUID;

public interface UserAdminUsecase {

    List<UserAdminResult> list(int page, int size);

    UserAdminResult get(UUID userId);

    UserAdminResult create(CreateUserCommand cmd, RequestMetadata metadata);

    UserAdminResult update(UUID userId, UpdateUserCommand cmd, RequestMetadata metadata);

    void delete(UUID userId, RequestMetadata metadata);
}