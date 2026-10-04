package com.tip_video_ludoteca.users;

public record UserProfileResponse(
        Long id,
        String username,
        String description,
        String avatarUrl) {

    static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDescription(),
                user.getProfileImageUrl());
    }
}
