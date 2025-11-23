package dev.harscode.itsectest.adapters.web.user;


import dev.harscode.itsectest.adapters.web.user.dto.UserAdminResult;
import dev.harscode.itsectest.adapters.web.user.dto.UserResponse;
import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.crypt.PiiCrypto;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class UserWebMapper {

    private final PiiCrypto piiCrypto;

    public UserWebMapper(PiiCrypto piiCrypto) {
        this.piiCrypto = piiCrypto;
    }

    public UserResponse toResponse(UserAdminResult result) {
        User u = result.user();
        UserProfile p = result.profile();

        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                p.getFullName(),
                u.getRole(),
                u.getStatus(),
                u.isEmailVerified(),
                u.isMfaEnabled(),
                u.getLastLoginAt(),
                u.getCreatedAt()
        );
    }
}
