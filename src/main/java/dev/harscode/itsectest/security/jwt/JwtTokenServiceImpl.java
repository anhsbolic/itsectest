package dev.harscode.itsectest.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenServiceImpl implements JwtTokenService {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenServiceImpl(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = buildKey(properties.getSecret());
    }

    private SecretKey buildKey(String secret) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateAccessToken(String userId, String sessionId, String role, boolean mfa) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.getAccessTokenTtlMinutes()));

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .header()
                .type("JWT")
                .and()
                .claims()
                .subject(userId)
                .id(jti)
                .issuer(properties.getIssuer())
                .audience().add(properties.getAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .add("sid", sessionId)
                .add("typ", "access")
                .add("role", role)
                .add("mfa", mfa)
                .and()
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public AccessTokenPayload parseAndValidateAccessToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.getIssuer())
                    .requireAudience(properties.getAudience())
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();

            String typ = claims.get("typ", String.class);
            if (!"access".equals(typ)) {
                return null;
            }

            String userId = claims.getSubject();
            String sessionId = claims.get("sid", String.class);
            String jti = claims.getId();
            String role = claims.get("role", String.class);
            Boolean mfa = claims.get("mfa", Boolean.class);

            if (userId == null || sessionId == null || jti == null || role == null) {
                return null;
            }

            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())
            );

            return new AccessTokenPayload(
                    userId,
                    sessionId,
                    jti,
                    role,
                    mfa != null && mfa,
                    authorities
            );
        } catch (ExpiredJwtException ex) {
            return null;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}