package dev.harscode.itsectest.ports;

import dev.harscode.itsectest.domain.auth.UserToken;

import java.util.Optional;
import java.util.UUID;

public interface UserTokenRepository {

    UserToken create(UserToken token);

    Optional<UserToken> findValidToken(String tokenHash, String tokenType);

    void markUsed(UUID tokenId);
}