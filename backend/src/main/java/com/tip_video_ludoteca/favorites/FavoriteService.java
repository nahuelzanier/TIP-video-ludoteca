package com.tip_video_ludoteca.favorites;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class FavoriteService {

    private final FavoriteGameRepository favoriteGames;
    private final GameRepository games;
    private final UserRepository users;

    public FavoriteService(
            FavoriteGameRepository favoriteGames,
            GameRepository games,
            UserRepository users) {
        this.favoriteGames = favoriteGames;
        this.games = games;
        this.users = users;
    }

    @Transactional
    public FavoriteGame addFavorite(String gameId, String userEmail) {
        Game game = games.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);

        return favoriteGames.findByUser_IdAndGame_Id(user.getId(), gameId)
                .orElseGet(() -> favoriteGames.save(new FavoriteGame(user, game)));
    }

    @Transactional
    public void removeFavorite(String gameId, String userEmail) {
        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);

        favoriteGames.findByUser_IdAndGame_Id(user.getId(), gameId)
                .ifPresent(favoriteGames::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(String gameId, String userEmail) {
        return favoriteGames.existsByUser_EmailIgnoreCaseAndGame_Id(userEmail, gameId);
    }

    @Transactional(readOnly = true)
    public Page<Game> listFavorites(String userEmail, Pageable pageable) {
        return favoriteGames.findGamesByUserEmailAndStatus(
                userEmail,
                GameStatus.PUBLISHED,
                pageable);
    }

    @Transactional(readOnly = true)
    public Set<String> findFavoriteGameIdsForUser(String userEmail) {
        return favoriteGames.findGameIdsByUserEmail(userEmail);
    }
}
