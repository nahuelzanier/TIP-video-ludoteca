package com.tip_video_ludoteca.forums;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ForumRatingRepository
        extends JpaRepository<ForumRating, ForumRatingId> {

    Optional<ForumRating> findByDiscussion_IdAndUser_Id(
            Long discussionId,
            Long userId);

    @Query("""
            SELECT r.discussion.id AS discussionId,
                   AVG(r.rating) AS averageRating,
                   COUNT(r) AS ratingCount
            FROM ForumRating r
            WHERE r.discussion.id IN :discussionIds
            GROUP BY r.discussion.id
            """)
    List<ForumRatingAggregate> aggregateByDiscussionIds(
            @Param("discussionIds") Collection<Long> discussionIds);
}