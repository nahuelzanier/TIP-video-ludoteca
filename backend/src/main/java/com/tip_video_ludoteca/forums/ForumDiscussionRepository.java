package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ForumDiscussionRepository
        extends JpaRepository<ForumDiscussion, Long> {

    Page<ForumDiscussion> findByGame_Status(
            GameStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {"author", "game", "tags"})
    Optional<ForumDiscussion> findWithDetailsById(Long id);
}