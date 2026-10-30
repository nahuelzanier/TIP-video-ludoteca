package com.tip_video_ludoteca.forums;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RateForumDiscussionRequest(
        @NotNull
        @Min(1)
        @Max(5)
        Integer rating
) {
}