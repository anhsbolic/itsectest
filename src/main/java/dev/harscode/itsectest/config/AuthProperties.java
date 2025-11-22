package dev.harscode.itsectest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String emailVerificationBaseUrl;
    private long emailVerificationTtlHours;

    public String getEmailVerificationBaseUrl() {
        return emailVerificationBaseUrl;
    }

    public void setEmailVerificationBaseUrl(String emailVerificationBaseUrl) {
        this.emailVerificationBaseUrl = emailVerificationBaseUrl;
    }

    public long getEmailVerificationTtlHours() {
        return emailVerificationTtlHours;
    }

    public void setEmailVerificationTtlHours(long emailVerificationTtlHours) {
        this.emailVerificationTtlHours = emailVerificationTtlHours;
    }

    public String buildEmailVerificationLink(String token) {
        return emailVerificationBaseUrl + "?token=" + token;
    }
}
