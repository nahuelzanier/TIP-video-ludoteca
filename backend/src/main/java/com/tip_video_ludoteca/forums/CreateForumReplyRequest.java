package com.tip_video_ludoteca.forums;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateForumReplyRequest(
    @NotBlank
    @Size(max = 3000)
    String content,

    Long parentReplyId
) {}