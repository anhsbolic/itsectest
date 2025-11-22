package dev.harscode.itsectest.ports;

public interface PiiCrypto {
    byte[] encrypt(String plaintext);

    String decrypt(byte[] ciphertext);

    String hash(String value);
}
