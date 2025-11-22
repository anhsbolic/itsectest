package dev.harscode.itsectest.ports;

public interface MailSenderPort {
    void sendEmailVerification(String toEmail, String verificationLink);
}
