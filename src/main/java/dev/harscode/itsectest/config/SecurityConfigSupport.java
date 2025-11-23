package dev.harscode.itsectest.config;

import dev.harscode.itsectest.security.jwt.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AuthProperties.class,
        LoginAttemptProperties.class,
        JwtProperties.class,
        PiiCryptoProperties.class
})
public class SecurityConfigSupport {
}
