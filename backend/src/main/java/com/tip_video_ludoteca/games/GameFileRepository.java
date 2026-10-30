package com.tip_video_ludoteca.games;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameFileRepository
        extends JpaRepository<GameFile, GameFileId> {

    @Query("""
            SELECT file
            FROM GameFile file
            WHERE file.game.id = :gameId
              AND file.game.status = com.tip_video_ludoteca.games.GameStatus.PUBLISHED
              AND file.id.path = :path
            """)
    Optional<GameFile> findPublishedByGameIdAndPath(
            @Param("gameId") String gameId,
            @Param("path") String path);

    List<GameFile> findAllById_GameId(String gameId);
}
