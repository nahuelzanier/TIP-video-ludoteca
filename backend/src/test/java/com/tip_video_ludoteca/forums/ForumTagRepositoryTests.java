package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ForumTagRepositoryTests {

    @Autowired
    private ForumTagRepository tags;

    @Autowired
    private ForumDiscussionRepository discussions;

    @Autowired
    private GameRepository games;

    @Autowired
    private UserRepository users;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void seededForumTagsAreAvailableInAlphabeticalOrder() {
        assertThat(tags.findAllByOrderByNameAsc())
                .extracting(ForumTag::getName)
                .contains("Bug", "Discussion", "Experience", "Review", "Technical issue")
                .isSorted();
    }

    @Test
    void discussionPersistsItsForumTagsAndLegacyGameTagJoinTableIsRemoved() {
        User author = users.save(new User(
                "forum-tag-author",
                "forum-tag-author@test.com",
                "{noop}hash"));
        Game game = games.save(new Game(
                "forum-tag-game",
                author,
                "Forum tag test",
                "",
                null,
                "index.html",
                GameStatus.PUBLISHED));
        ForumTag experience = tags.findAllByOrderByNameAsc().stream()
                .filter(tag -> tag.getSlug().equals("experience"))
                .findFirst()
                .orElseThrow();
        ForumDiscussion discussion = new ForumDiscussion(
                game, author, "My experience", "A forum discussion", null, null);
        discussion.getTags().add(experience);

        ForumDiscussion saved = discussions.saveAndFlush(discussion);
        entityManager.clear();

        ForumDiscussion loaded = discussions.findWithDetailsById(saved.getId()).orElseThrow();
        assertThat(loaded.getTags())
                .extracting(ForumTag::getSlug)
                .containsExactly("experience");

        String legacyTable = jdbcTemplate.queryForObject(
                "SELECT to_regclass('forum_discussion_tags')",
                String.class);
        assertThat(legacyTable).isNull();
    }

    @Test
    void authorDiscussionsIncludeOnlyPublishedGamesAndMatchUsernameIgnoringCase() {
        User author = users.save(new User(
                "forum-profile-author",
                "forum-profile-author@test.com",
                "{noop}hash"));
        User anotherAuthor = users.save(new User(
                "forum-profile-other",
                "forum-profile-other@test.com",
                "{noop}hash"));

        Game publishedGame = games.save(new Game(
                "forum-profile-published",
                author,
                "Published game",
                "",
                null,
                "index.html",
                GameStatus.PUBLISHED));
        Game draftGame = games.save(new Game(
                "forum-profile-draft",
                author,
                "Draft game",
                "",
                null,
                "index.html",
                GameStatus.DRAFT));
        Game anotherPublishedGame = games.save(new Game(
                "forum-profile-other-published",
                anotherAuthor,
                "Another published game",
                "",
                null,
                "index.html",
                GameStatus.PUBLISHED));

        discussions.saveAllAndFlush(List.of(
                new ForumDiscussion(publishedGame, author, "Visible", "description", null, null),
                new ForumDiscussion(draftGame, author, "Hidden", "description", null, null),
                new ForumDiscussion(anotherPublishedGame, anotherAuthor, "Other", "description", null, null)));

        assertThat(discussions.findByAuthorUsernameAndGameStatus(
                "FORUM-PROFILE-AUTHOR", GameStatus.PUBLISHED))
                .extracting(ForumDiscussion::getTitle)
                .containsExactly("Visible");
    }
}
