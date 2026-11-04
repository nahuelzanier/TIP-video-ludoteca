package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ForumDiscussionRepository
        extends JpaRepository<ForumDiscussion, Long>,
        JpaSpecificationExecutor<ForumDiscussion> {

    Page<ForumDiscussion> findByGame_Status(
            GameStatus status,
            Pageable pageable);

    @Query("""
            select discussion
            from ForumDiscussion discussion
            where lower(discussion.author.username) = lower(:username)
              and discussion.game.status = :status
            order by discussion.createdAt desc
            """)
    List<ForumDiscussion> findByAuthorUsernameAndGameStatus(
            @Param("username") String username,
            @Param("status") GameStatus status);

    @EntityGraph(attributePaths = {"author", "game", "tags"})
    Optional<ForumDiscussion> findWithDetailsById(Long id);
}
