package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCustomizationService {

    private static final String DEFAULT_BACKGROUND = "#17151f";
    private static final String DEFAULT_ACCENT = "#a36aff";
    private static final String DEFAULT_TEXT = "#f5f4f8";
    private static final String DEFAULT_FONT = "SYSTEM";
    private static final String DEFAULT_BACKGROUND_STYLE = "SOLID";

    private final GameRepository games;
    private final GameCustomizationRepository customizations;
    private final UserRepository users;

    public GameCustomizationService(
            GameRepository games,
            GameCustomizationRepository customizations,
            UserRepository users) {
        this.games = games;
        this.customizations = customizations;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Customization get(String gameId) {
        Game game = games.findById(gameId).orElseThrow(GameNotFoundException::new);
        if (game.getStatus() != GameStatus.PUBLISHED) {
            throw new GameNotFoundException();
        }
        return customizations.findByGameId(gameId)
                .map(GameCustomizationService::toResponse)
                .orElseGet(GameCustomizationService::defaults);
    }

    @Transactional
    public Customization update(String gameId, String currentUserEmail,
            UpdateGameCustomizationRequest request) {
        Game game = games.findById(gameId).orElseThrow(GameNotFoundException::new);
        User currentUser = users.findByEmailIgnoreCase(currentUserEmail)
                .orElseThrow(GameEditForbiddenException::new);
        if (!game.getOwner().getId().equals(currentUser.getId())) {
            throw new GameEditForbiddenException();
        }

        GameCustomization customization = customizations.findByGameId(gameId)
                .orElseGet(() -> new GameCustomization(game.getId(),
                        DEFAULT_BACKGROUND, DEFAULT_ACCENT, DEFAULT_TEXT, DEFAULT_FONT,
                        DEFAULT_BACKGROUND_STYLE));
        customization.update(request.backgroundColor(), request.accentColor(), request.textColor(),
                request.fontFamily(), request.backgroundStyle());
        return toResponse(customizations.save(customization));
    }

    private static Customization defaults() {
        return new Customization(DEFAULT_BACKGROUND, DEFAULT_ACCENT, DEFAULT_TEXT,
                DEFAULT_FONT, DEFAULT_BACKGROUND_STYLE);
    }

    private static Customization toResponse(GameCustomization customization) {
        return new Customization(customization.getBackgroundColor(),
                customization.getAccentColor(), customization.getTextColor(), customization.getFontFamily(),
                customization.getBackgroundStyle());
    }

    public record Customization(
            String backgroundColor,
            String accentColor,
            String textColor,
            String fontFamily,
            String backgroundStyle) { }
}
