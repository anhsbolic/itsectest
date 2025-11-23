package dev.harscode.itsectest.adapters.mail;

import dev.harscode.itsectest.ports.mail.MailSenderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SmtpMailSenderAdapter implements MailSenderPort {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSenderAdapter.class);

    private final JavaMailSender mailSender;

    public SmtpMailSenderAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendEmailVerification(String toEmail, String verificationLink) {
        log.info("[DEV MAIL] Sending email verification to {}: {}", toEmail, verificationLink);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("Verify your account");
        msg.setText("Silakan klik link berikut untuk verifikasi akun kamu:\n\n" + verificationLink);

        mailSender.send(msg);
    }

    @Override
    public void sendMfaOtp(String to, String otp) {
        log.info("[DEV MAIL] Sending MFA OTP to {}: {}", to, otp);

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Your MFA OTP");
        msg.setText("Kode OTP kamu: " + otp + " (berlaku 5 menit)");

        mailSender.send(msg);
    }
}