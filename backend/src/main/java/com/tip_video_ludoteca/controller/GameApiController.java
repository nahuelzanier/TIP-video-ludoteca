package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
public class GameApiController {

    private final GameRepository games;

    public GameApiController(GameRepository games) {
        this.games = games;
    }

    @GetMapping
    public List<Game> getGames() {
        return games.findAll();
    }
}
