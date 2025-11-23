package dev.harscode.itsectest.ports.mail;

public interface MailSenderPort {
    void sendEmailVerification(String toEmail, String verificationLink);
}
