package com.tip_video_ludoteca.favorites;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface FavoriteGameRepository extends JpaRepository<FavoriteGame, FavoriteGameId> {

    Optional<FavoriteGame> findByUser_IdAndGame_Id(Long userId, String gameId);

    boolean existsByUser_EmailIgnoreCaseAndGame_Id(String email, String gameId);

    @Query("SELECT f.game FROM FavoriteGame f WHERE f.user.email = :email AND f.game.status = :status ORDER BY f.createdAt DESC")
    Page<Game> findGamesByUserEmailAndStatus(
            @Param("email") String email,
            @Param("status") GameStatus status,
            Pageable pageable);

    @Query("SELECT f.game.id FROM FavoriteGame f WHERE f.user.email = :email")
    Set<String> findGameIdsByUserEmail(@Param("email") String email);
}
