package com.tip_video_ludoteca.forums;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ForumTagService {

    private final ForumTagRepository tags;

    public ForumTagService(ForumTagRepository tags) {
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    public List<ForumTagOption> listTags() {
        return tags.findAllByOrderByNameAsc().stream()
                .map(tag -> new ForumTagOption(
                        tag.getId(),
                        tag.getName(),
                        tag.getSlug()))
                .toList();
    }

    public record ForumTagOption(Long id, String name, String slug) {
    }
}
