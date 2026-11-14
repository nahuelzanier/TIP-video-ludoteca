package com.tip_video_ludoteca.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private short volume = 100;

    @Column(nullable = false, length = 5)
    private String language = "en";

    @Column(nullable = false, length = 10)
    private String theme = "system";

    protected UserSettings() {
    }

    public UserSettings(User user, int volume, String language, String theme) {
        this.user = user;
        update(volume, language, theme);
    }

    public void update(int volume, String language, String theme) {
        this.volume = (short) volume;
        this.language = language;
        this.theme = theme;
    }

    public Long getUserId() {
        return userId != null ? userId : user.getId();
    }

    public int getVolume() {
        return volume;
    }

    public String getLanguage() {
        return language;
    }

    public String getTheme() {
        return theme;
    }
}
