package dev.harscode.itsectest.security.jwt;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;

public interface JwtTokenService {

    /**
     * Generate an access token (JWT) for a user session.
     */
    String generateAccessToken(String userId, String sessionId, String role, boolean mfa);

    /**
     * Parse & validate access token.
     * Returns payload if valid, or null if invalid/expired.
     */
    AccessTokenPayload parseAndValidateAccessToken(String token);

    record AccessTokenPayload(
            String userId,
            String sessionId,
            String jti,
            String role,
            boolean mfa,
            List<GrantedAuthority> authorities
    ) {
    }
}
