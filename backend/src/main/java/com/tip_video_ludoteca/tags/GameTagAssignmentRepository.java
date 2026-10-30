package com.tip_video_ludoteca.tags;

import com.tip_video_ludoteca.games.GameRecommendationSummary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface GameTagAssignmentRepository
        extends JpaRepository<GameTagAssignment, GameTagAssignmentId> {

    boolean existsById_GameIdAndId_TagIdAndId_UserId(
            String gameId,
            Long tagId,
            Long userId
    );

    List<GameTagAssignment> findAllById_GameId(String gameId);

    @Query("""
            SELECT new com.tip_video_ludoteca.tags.GameTagSummary(
                tag.id,
                tag.name,
                tag.slug,
                COUNT(assignment.id.userId),
                MIN(assignment.assignedAt)
            )
            FROM GameTagAssignment assignment
            JOIN assignment.tag tag
            WHERE assignment.id.gameId = :gameId
            GROUP BY tag.id, tag.name, tag.slug
            ORDER BY COUNT(assignment.id.userId) DESC,
                    MIN(assignment.assignedAt) ASC,
                    tag.name ASC
            """)
        List<GameTagSummary> findTagSummariesByGameId(
                @Param("gameId") String gameId
    );


    @Query("""
            SELECT assignment.id.tagId
            FROM GameTagAssignment assignment
            WHERE assignment.id.gameId = :gameId
            AND assignment.id.userId = :userId
            """)
        List<Long> findAssignedTagIdsByGameIdAndUserId(
                @Param("gameId") String gameId,
                @Param("userId") Long userId
    );

    @Query("""
            SELECT new com.tip_video_ludoteca.games.GameRecommendationSummary(
                game.id,
                game.title,
                game.coverImageUrl)
            FROM GameTagAssignment assignment
            JOIN assignment.game game
            WHERE assignment.id.gameId <> :gameId
            AND game.status = com.tip_video_ludoteca.games.GameStatus.PUBLISHED
            AND assignment.id.tagId IN (
                SELECT other.id.tagId
                FROM GameTagAssignment other
                WHERE other.id.gameId = :gameId
            )
            GROUP BY game.id, game.title, game.coverImageUrl
            HAVING COUNT(DISTINCT assignment.id.tagId) >= 2
            ORDER BY COUNT(DISTINCT assignment.id.tagId) DESC,
                    game.title ASC,
                    game.id ASC
            """)
        List<GameRecommendationSummary> findRecommendationsForGame(
                @Param("gameId") String gameId,
                Pageable pageable
    );
}