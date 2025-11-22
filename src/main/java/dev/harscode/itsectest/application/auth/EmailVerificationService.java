package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.domain.auth.UserToken;
import dev.harscode.itsectest.ports.UserRepository;
import dev.harscode.itsectest.ports.UserTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class EmailVerificationService implements EmailVerificationUsecase {

    private final UserTokenRepository userTokenRepository;
    private final UserRepository userRepository;

    public EmailVerificationService(UserTokenRepository userTokenRepository, UserRepository userRepository) {
        this.userTokenRepository = userTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void verify(String token) {
        UserToken userToken = userTokenRepository.findValidToken(token, "email-verification").orElseThrow(() -> new IllegalArgumentException("Invalid or expired token"));

        // mark token as used
        userTokenRepository.markUsed(userToken.getId());

        // update user
        userRepository.markEmailVerifiedAndActivate(userToken.getUserId());
    }
}
