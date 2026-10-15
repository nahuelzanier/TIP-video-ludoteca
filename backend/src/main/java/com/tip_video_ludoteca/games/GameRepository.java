package com.tip_video_ludoteca.games;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface GameRepository extends JpaRepository<Game, String> {
    Page<Game> findByStatus(GameStatus status, Pageable pageable);
    List<Game> findByStatusOrderByCreatedAtDesc(GameStatus status);
    List<Game> findByOwner_IdOrderByCreatedAtDesc(Long ownerId);
    List<Game> findByOwner_IdNot(Long ownerId);
    Page<Game> findByTitleContainingIgnoreCase(String title, Pageable pageable);
    Page<Game> findByOwner_UsernameIgnoreCaseAndStatus(
        String username,
        GameStatus status,
        Pageable pageable);

    @Query(
        value = "SELECT * FROM games WHERE status = :status ORDER BY random() LIMIT 1",
        nativeQuery = true
    )
    Optional<Game> findRandomByStatus(@Param("status") String status);
    Page<Game> findByStatusAndTitleContainingIgnoreCase(
        GameStatus status,
        String title,
        Pageable pageable
    );
}
