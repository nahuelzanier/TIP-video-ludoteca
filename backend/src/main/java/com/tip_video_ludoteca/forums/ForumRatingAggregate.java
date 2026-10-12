package com.tip_video_ludoteca.forums;

public interface ForumRatingAggregate {
    Long getDiscussionId();
    Double getAverageRating();
    Long getRatingCount();
}