package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private final GameRepository games;
    private final UserRepository users;

    public GameService(GameRepository games, UserRepository users) {
        this.games = games;
        this.users = users;
    }

    @Transactional
    public PublishResult publish(String gameId, String userEmail) {
        Game game = games.findById(gameId)
                .orElseThrow(GameNotFoundException::new);

        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);

        if (!game.getOwner().getId().equals(user.getId())) {
            throw new GameEditForbiddenException();
        }

        game.publish();
        games.save(game);

        return new PublishResult(game.getId(), game.getTitle(), game.getStatus());
    }

    public record PublishResult(
            String id,
            String title,
            GameStatus status) {
    }
}