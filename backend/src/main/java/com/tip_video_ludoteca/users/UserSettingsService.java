package com.tip_video_ludoteca.users;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Service
public class UserSettingsService {

    private final UserRepository users;
    private final UserSettingsRepository settings;

    public UserSettingsService(UserRepository users, UserSettingsRepository settings) {
        this.users = users;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get(String userEmail) {
        User user = findUser(userEmail);
        return settings.findById(user.getId())
                .map(UserSettingsService::toResponse)
                .orElseGet(SettingsResponse::defaults);
    }

    @Transactional
    public SettingsResponse update(String userEmail, UpdateSettingsRequest request) {
        User user = findUser(userEmail);
        UserSettings value = settings.findById(user.getId())
                .orElseGet(() -> new UserSettings(user, request.volume(),
                        request.language(), request.theme()));
        value.update(request.volume(), request.language(), request.theme());
        return toResponse(settings.save(value));
    }

    private User findUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(UserNotFoundException::new);
    }

    private static SettingsResponse toResponse(UserSettings settings) {
        return new SettingsResponse(
                settings.getVolume(), settings.getLanguage(), settings.getTheme());
    }

    public record UpdateSettingsRequest(
            @Min(0) @Max(100) int volume,
            @NotBlank @Pattern(regexp = "en|es") String language,
            @NotBlank @Pattern(regexp = "system|light|dark") String theme) {
    }

    public record SettingsResponse(int volume, String language, String theme) {
        public static SettingsResponse defaults() {
            return new SettingsResponse(100, "en", "system");
        }
    }
}
