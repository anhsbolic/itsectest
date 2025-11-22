package dev.harscode.itsectest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.pii")
public class PiiCryptoProperties {

    /**
     * 256-bit key encoded as Base64 (32 bytes after decoding)
     */
    private String key;

    /**
     * Extra salt for hashing PII (string)
     */
    private String hashSalt = "change-this-salt";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHashSalt() {
        return hashSalt;
    }

    public void setHashSalt(String hashSalt) {
        this.hashSalt = hashSalt;
    }
}
