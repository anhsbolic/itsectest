package dev.harscode.itsectest.security.token;

public interface TokenHashService {
    String hash(String token);
    String generateRefreshToken();
}
