package com.tip_video_ludoteca.games;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "game_customizations")
public class GameCustomization {

    @Id
    @Column(name = "game_id", length = 36)
    private String gameId;

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;

    @Column(name = "accent_color", nullable = false, length = 7)
    private String accentColor;

    @Column(name = "text_color", nullable = false, length = 7)
    private String textColor;

    @Column(name = "font_family", nullable = false, length = 16)
    private String fontFamily;

    @Column(name = "background_style", nullable = false, length = 16)
    private String backgroundStyle;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected GameCustomization() { }

    public GameCustomization(
            String gameId,
            String backgroundColor,
            String accentColor,
            String textColor,
            String fontFamily,
            String backgroundStyle) {
        this.gameId = gameId;
        update(backgroundColor, accentColor, textColor, fontFamily, backgroundStyle);
    }

    public void update(
            String backgroundColor,
            String accentColor,
            String textColor,
            String fontFamily,
            String backgroundStyle) {
        this.backgroundColor = backgroundColor;
        this.accentColor = accentColor;
        this.textColor = textColor;
        this.fontFamily = fontFamily;
        this.backgroundStyle = backgroundStyle;
        this.updatedAt = Instant.now();
    }

    public String getGameId() { return gameId; }
    public String getBackgroundColor() { return backgroundColor; }
    public String getAccentColor() { return accentColor; }
    public String getTextColor() { return textColor; }
    public String getFontFamily() { return fontFamily; }
    public String getBackgroundStyle() { return backgroundStyle; }
    public Instant getUpdatedAt() { return updatedAt; }
}
