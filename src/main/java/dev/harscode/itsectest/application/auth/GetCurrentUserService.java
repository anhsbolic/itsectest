package dev.harscode.itsectest.application.auth;

import dev.harscode.itsectest.domain.user.User;
import dev.harscode.itsectest.domain.user.UserProfile;
import dev.harscode.itsectest.ports.UserProfileRepository;
import dev.harscode.itsectest.ports.UserRepository;
import dev.harscode.itsectest.web.exception.UnauthorizedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCurrentUserService implements GetCurrentUserUsecase {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    public GetCurrentUserService(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public GetCurrentUserResult getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getStatus().equals("active") || !user.isEmailVerified()) {
            throw new UnauthorizedException("User is not active or email not verified");
        }

        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);

        return new GetCurrentUserResult(user, profile);
    }
}
