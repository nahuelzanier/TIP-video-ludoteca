package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.forums.ForumTagService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forum/tags")
public class ForumTagController {

    private final ForumTagService tags;

    public ForumTagController(ForumTagService tags) {
        this.tags = tags;
    }

    @GetMapping
    public List<ForumTagService.ForumTagOption> listTags() {
        return tags.listTags();
    }
}