package com.tip_video_ludoteca.comments;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByGame_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(String gameId);

    @EntityGraph(attributePaths = "author")
    List<Comment> findByGame_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(String gameId);
}