package com.tip_video_ludoteca.news;

import com.tip_video_ludoteca.followers.Follow;
import com.tip_video_ludoteca.followers.FollowRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NoticiaRepositoryTests {

    @Autowired
    private NoticiaRepository noticias;

    @Autowired
    private FollowRepository follows;

    @Autowired
    private UserRepository users;

    @Test
    void feedOnlyIncludesAuthorsTheUserFollowsAndOrdersByDateDesc() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User viewer = users.save(user("viewer-" + suffix));
        User followed = users.save(user("followed-" + suffix));
        User stranger = users.save(user("stranger-" + suffix));

        follows.save(new Follow(viewer, followed));

        Noticia oldNews = noticias.save(new Noticia("Vieja", "Texto", followed));
        Noticia newNews = noticias.save(new Noticia("Nueva", "Texto", followed));
        noticias.save(new Noticia("Ajena", "Texto", stranger));

        ReflectionTestUtils.setField(oldNews, "publishedAt", Instant.parse("2026-01-01T10:00:00Z"));
        ReflectionTestUtils.setField(newNews, "publishedAt", Instant.parse("2026-02-01T10:00:00Z"));
        noticias.save(oldNews);
        noticias.save(newNews);

        Page<Noticia> feed = noticias.findFeedByFollowerEmail(
                viewer.getEmail(), PageRequest.of(0, 9));

        assertThat(feed.getContent()).extracting(Noticia::getTitle)
                .containsExactly("Nueva", "Vieja");
        assertThat(feed.getTotalElements()).isEqualTo(2);
    }

    private User user(String username) {
        return new User(username, username + "@test.com", "{bcrypt}hash");
    }
}
