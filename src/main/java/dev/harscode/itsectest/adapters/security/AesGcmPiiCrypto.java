package dev.harscode.itsectest.adapters.security;

import dev.harscode.itsectest.config.PiiCryptoProperties;
import dev.harscode.itsectest.ports.PiiCrypto;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmPiiCrypto implements PiiCrypto {

    private static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final SecretKey key;
    private final String hashSalt;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmPiiCrypto(PiiCryptoProperties props) {
        if (props.getKey() == null) {
            throw new IllegalStateException("security.pii.key must be configured (Base64 256-bit key)");
        }
        byte[] keyBytes = Base64.getDecoder().decode(props.getKey());
        if (keyBytes.length != 32) {
            throw new IllegalStateException("security.pii.key must be 32 bytes (256-bit)");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.hashSalt = props.getHashSalt() != null ? props.getHashSalt() : "";
    }

    @Override
    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return buffer.array();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt PII", e);
        }
    }

    @Override
    public String decrypt(byte[] data) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt PII", e);
        }
    }

    @Override
    public String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String salted = hashSalt + ":" + value;
            byte[] hashBytes = digest.digest(salted.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash PII", e);
        }
    }
}
