package com.tip_video_ludoteca.forums;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForumReplyRepository
        extends JpaRepository<ForumReply, Long> {

    @EntityGraph(attributePaths = "author")
    List<ForumReply> findByDiscussion_IdOrderByCreatedAtAscIdAsc(
            Long discussionId);

    Optional<ForumReply> findByIdAndDiscussion_Id(
            Long replyId,
            Long discussionId);
}