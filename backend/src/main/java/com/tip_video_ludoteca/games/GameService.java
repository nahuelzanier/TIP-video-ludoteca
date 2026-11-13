package com.tip_video_ludoteca.games;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameService {

    private final GameRepository games;
    public GameService(GameRepository games) {
        this.games = games;
    }

    @Transactional
    public PublishResult publishAsAdmin(String gameId) {
        Game game = games.findById(gameId)
                .orElseThrow(GameNotFoundException::new);
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
