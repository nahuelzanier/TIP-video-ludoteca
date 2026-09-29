package com.tip_video_ludoteca.games;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, String> {
    List<Game> findByStatusOrderByCreatedAtDesc(GameStatus status);

    List<Game> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
}