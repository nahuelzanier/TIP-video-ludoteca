package com.tip_video_ludoteca.tags;

import java.time.Instant;

public record GameTagSummary(
        Long tagId,
        String name,
        String slug,
        Long assignmentCount,
        Instant firstAssignedAt
) {
}