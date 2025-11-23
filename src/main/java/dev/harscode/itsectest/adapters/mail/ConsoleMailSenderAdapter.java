package dev.harscode.itsectest.adapters.mail;

import dev.harscode.itsectest.ports.mail.MailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleMailSenderAdapter implements MailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleMailSenderAdapter.class);

    @Override
    public void sendEmailVerification(String toEmail, String verificationLink) {
        log.info("[DEV MAIL] Sending email verification to {}: {}", toEmail, verificationLink);
    }
}
