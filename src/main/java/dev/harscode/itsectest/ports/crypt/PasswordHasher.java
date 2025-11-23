package dev.harscode.itsectest.ports.crypt;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
