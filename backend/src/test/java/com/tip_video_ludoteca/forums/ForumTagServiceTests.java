package com.tip_video_ludoteca.forums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumTagServiceTests {

    @Mock
    private ForumTagRepository tags;

    @InjectMocks
    private ForumTagService forumTags;

    @Test
    void listsTheForumCatalogInRepositoryOrder() {
        ForumTag bug = new ForumTag();
        ReflectionTestUtils.setField(bug, "id", 2L);
        ReflectionTestUtils.setField(bug, "name", "Bug");
        ReflectionTestUtils.setField(bug, "slug", "bug");
        when(tags.findAllByOrderByNameAsc()).thenReturn(List.of(bug));

        assertThat(forumTags.listTags())
                .containsExactly(new ForumTagService.ForumTagOption(2L, "Bug", "bug"));
        verify(tags).findAllByOrderByNameAsc();
    }
}