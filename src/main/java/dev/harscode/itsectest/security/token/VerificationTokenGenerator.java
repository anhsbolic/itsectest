package dev.harscode.itsectest.security.token;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class VerificationTokenGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generateRandomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}