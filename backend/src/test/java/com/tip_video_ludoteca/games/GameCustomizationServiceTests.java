package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCustomizationServiceTests {

    private static final String GAME_ID = "game-1";
    @Mock private GameRepository games;
    @Mock private GameCustomizationRepository customizations;
    @Mock private UserRepository users;
    private GameCustomizationService service;

    @BeforeEach
    void setUp() {
        service = new GameCustomizationService(games, customizations, users);
    }

    @Test
    void returnsDefaultsForPublishedGameWithoutCustomization() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game("creator", GameStatus.PUBLISHED)));
        when(customizations.findByGameId(GAME_ID)).thenReturn(Optional.empty());

        GameCustomizationService.Customization result = service.get(GAME_ID);

        assertThat(result.backgroundColor()).isEqualTo("#17151f");
        assertThat(result.accentColor()).isEqualTo("#a36aff");
        assertThat(result.fontFamily()).isEqualTo("SYSTEM");
    }

    @Test
    void savesChangesForTheGameOwner() {
        Game game = game("creator", GameStatus.PUBLISHED);
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase("creator@test.com")).thenReturn(Optional.of(user("creator", "creator@test.com", 7L)));
        when(customizations.findByGameId(GAME_ID)).thenReturn(Optional.empty());
        when(customizations.save(org.mockito.ArgumentMatchers.any(GameCustomization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UpdateGameCustomizationRequest request = new UpdateGameCustomizationRequest(
                "#102030", "#d080ff", "#eeeeee", "SERIF", "GRADIENT");

        GameCustomizationService.Customization result = service.update(GAME_ID, "creator@test.com", request);

        assertThat(result.backgroundColor()).isEqualTo("#102030");
        assertThat(result.accentColor()).isEqualTo("#d080ff");
        assertThat(result.textColor()).isEqualTo("#eeeeee");
        assertThat(result.fontFamily()).isEqualTo("SERIF");
        assertThat(result.backgroundStyle()).isEqualTo("GRADIENT");
        ArgumentCaptor<GameCustomization> saved = ArgumentCaptor.forClass(GameCustomization.class);
        verify(customizations).save(saved.capture());
        assertThat(saved.getValue().getGameId()).isEqualTo(GAME_ID);
    }

    @Test
    void rejectsCustomizationByAnotherUser() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game("creator", GameStatus.PUBLISHED)));
        when(users.findByEmailIgnoreCase("someone-else@test.com")).thenReturn(Optional.of(user("someone-else", "someone-else@test.com", 8L)));
        UpdateGameCustomizationRequest request = new UpdateGameCustomizationRequest(
                "#102030", "#d080ff", "#eeeeee", "SYSTEM", "SOLID");

        assertThatThrownBy(() -> service.update(GAME_ID, "someone-else@test.com", request))
                .isInstanceOf(GameEditForbiddenException.class);
    }

    private Game game(String username, GameStatus status) {
        User owner = user(username, username + "@test.com", 7L);
        return new Game(GAME_ID, owner, "Aventura", "", null, "index.html", status);
    }

    private User user(String username, String email, long id) {
        User user = new User(username, email, "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
