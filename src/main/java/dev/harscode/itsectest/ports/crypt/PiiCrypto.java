package dev.harscode.itsectest.ports.crypt;

public interface PiiCrypto {
    byte[] encrypt(String plaintext);

    String decrypt(byte[] ciphertext);

    String hash(String value);
}
