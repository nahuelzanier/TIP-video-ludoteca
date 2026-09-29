package com.tip_video_ludoteca.games;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameFileRepository
        extends JpaRepository<GameFile, GameFileId> {

    Optional<GameFile> findById_GameIdAndId_Path(String gameId, String path);

    List<GameFile> findAllById_GameId(String gameId);
}