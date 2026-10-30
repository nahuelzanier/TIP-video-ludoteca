package com.tip_video_ludoteca.users;

import org.springframework.stereotype.Service;

@Service
public class UserProfileService {

    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final UserRepository users;

    public UserProfileService(UserRepository users) {
        this.users = users;
    }

    public UserProfileResponse getProfile(String username) {
        return UserProfileResponse.from(
                users.findByUsernameIgnoreCase(username)
                        .orElseThrow(UserNotFoundException::new));
    }

    public UserProfileResponse updateDescription(
            String authenticatedEmail,
            String username,
            String description) {

        User user = users.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getUsername().equalsIgnoreCase(username)) {
            throw new ProfileEditForbiddenException();
        }

        user.updateDescription(normalize(description));

        return UserProfileResponse.from(users.save(user));
    }

    private String normalize(String description) {
        if (description == null) {
            return null;
        }

        String trimmed = description.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
