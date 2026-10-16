package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.tags.Tag;
import com.tip_video_ludoteca.tags.TagRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForumDiscussionServiceTests {

    private static final Long DISCUSSION_ID = 12L;
    private static final Long USER_ID = 7L;
    private static final Long TAG_ID = 3L;
    private static final String GAME_ID = "game-1";
    private static final String EMAIL = "player@ludarium.test";

    @Mock
    private ForumDiscussionRepository discussions;

    @Mock
    private GameRepository games;

    @Mock
    private UserRepository users;

    @Mock
    private TagRepository tags;

    @Mock
    private ForumRatingRepository ratings;

    @Mock
    private ForumReplyRepository replies;

    @InjectMocks
    private ForumDiscussionService forum;

    @Test
    void createTrimsTextAndAllowsAnOptionalCover() {
        givenPublishedGame();
        givenUser();
        givenTag();
        when(discussions.save(any(ForumDiscussion.class))).thenAnswer(invocation -> {
            ForumDiscussion saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", DISCUSSION_ID);
            return saved;
        });

        ForumDiscussionService.CreatedDiscussion created = forum.create(
                GAME_ID, "  A useful title  ", "  Short description  ", null,
                List.of(TAG_ID), EMAIL);

        ArgumentCaptor<ForumDiscussion> captor = ArgumentCaptor.forClass(ForumDiscussion.class);
        verify(discussions).save(captor.capture());
        ForumDiscussion saved = captor.getValue();

        assertThat(created.id()).isEqualTo(DISCUSSION_ID);
        assertThat(created.title()).isEqualTo("A useful title");
        assertThat(saved.getDescription()).isEqualTo("Short description");
        assertThat(saved.getCoverImage()).isNull();
        assertThat(saved.getCoverImageContentType()).isNull();
        assertThat(saved.getTags()).extracting(Tag::getId).containsExactly(TAG_ID);
    }

    @Test
    void createRejectsAnUnpublishedGame() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.DRAFT)));

        assertThatThrownBy(() -> forum.create(
                GAME_ID, "Title", "Description", null, List.of(TAG_ID), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(discussions, never()).save(any(ForumDiscussion.class));
    }

    @Test
    void createRejectsDuplicateTagsBeforeSaving() {
        givenPublishedGame();
        givenUser();

        assertThatThrownBy(() -> forum.create(
                GAME_ID, "Title", "Description", null, List.of(TAG_ID, TAG_ID), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(tags, never()).findAllById(any());
        verify(discussions, never()).save(any(ForumDiscussion.class));
    }

    @Test
    void createRejectsInvalidImageBytes() {
        givenPublishedGame();
        givenUser();

        MockMultipartFile invalidImage = new MockMultipartFile(
                "cover", "cover.png", "image/png", "<script>".getBytes());

        assertThatThrownBy(() -> forum.create(
                GAME_ID, "Title", "Description", invalidImage, List.of(TAG_ID), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PNG o JPEG");

        verify(discussions, never()).save(any(ForumDiscussion.class));
    }

    @Test
    void createReplyTrimsContentAndAssociatesItsParent() {
        ForumDiscussion discussion = discussion(GameStatus.PUBLISHED);
        ForumReply parent = new ForumReply(discussion, user(), null, "Parent");
        ReflectionTestUtils.setField(parent, "id", 45L);
        when(discussions.findById(DISCUSSION_ID)).thenReturn(Optional.of(discussion));
        givenUser();
        when(replies.findByIdAndDiscussion_Id(45L, DISCUSSION_ID))
                .thenReturn(Optional.of(parent));
        when(replies.save(any(ForumReply.class))).thenAnswer(invocation -> {
            ForumReply saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 46L);
            return saved;
        });

        ForumDiscussionService.ForumReplyResponse response = forum.createReply(
                DISCUSSION_ID, new CreateForumReplyRequest("  Helpful reply  ", 45L), EMAIL);

        ArgumentCaptor<ForumReply> captor = ArgumentCaptor.forClass(ForumReply.class);
        verify(replies).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("Helpful reply");
        assertThat(captor.getValue().getParentReply().getId()).isEqualTo(45L);
        assertThat(response.parentReplyId()).isEqualTo(45L);
    }

    @Test
    void createReplyRejectsAParentFromAnotherDiscussion() {
        when(discussions.findById(DISCUSSION_ID))
                .thenReturn(Optional.of(discussion(GameStatus.PUBLISHED)));
        givenUser();
        when(replies.findByIdAndDiscussion_Id(99L, DISCUSSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> forum.createReply(
                DISCUSSION_ID, new CreateForumReplyRequest("Reply", 99L), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(replies, never()).save(any(ForumReply.class));
    }

    @Test
    void rateDiscussionCreatesOrUpdatesTheCurrentUsersRatingAndReturnsAggregate() {
        ForumDiscussion discussion = discussion(GameStatus.PUBLISHED);
        User user = user();
        when(discussions.findById(DISCUSSION_ID)).thenReturn(Optional.of(discussion));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(ratings.findByDiscussion_IdAndUser_Id(DISCUSSION_ID, USER_ID))
                .thenReturn(Optional.empty());
        when(ratings.aggregateByDiscussionIds(List.of(DISCUSSION_ID)))
                .thenReturn(List.of(aggregate(DISCUSSION_ID, 4.5, 2L)));

        ForumDiscussionService.RatingResult result = forum.rateDiscussion(
                DISCUSSION_ID, 5, EMAIL);

        ArgumentCaptor<ForumRating> captor = ArgumentCaptor.forClass(ForumRating.class);
        verify(ratings).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo((short) 5);
        assertThat(captor.getValue().getUser().getId()).isEqualTo(USER_ID);
        assertThat(result.averageRating()).isEqualTo(4.5);
        assertThat(result.ratingCount()).isEqualTo(2L);
    }

    @Test
    void rateDiscussionRejectsScoresOutsideOneThroughFive() {
        when(discussions.findById(DISCUSSION_ID))
                .thenReturn(Optional.of(discussion(GameStatus.PUBLISHED)));

        assertThatThrownBy(() -> forum.rateDiscussion(DISCUSSION_ID, 6, EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(users, never()).findByEmailIgnoreCase(EMAIL);
        verify(ratings, never()).saveAndFlush(any(ForumRating.class));
    }

    private void givenPublishedGame() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.PUBLISHED)));
    }

    private void givenUser() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
    }

    private void givenTag() {
        when(tags.findAllById(any())).thenReturn(List.of(tag()));
    }

    private Game game(GameStatus status) {
        return new Game(GAME_ID, user(), "Published game", "", null, "index.html", status);
    }

    private ForumDiscussion discussion(GameStatus status) {
        ForumDiscussion discussion = new ForumDiscussion(
                game(status), user(), "Discussion", "Description", null, null);
        ReflectionTestUtils.setField(discussion, "id", DISCUSSION_ID);
        return discussion;
    }

    private User user() {
        User user = new User("player", EMAIL, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        return user;
    }

    private Tag tag() {
        Tag tag = BeanUtils.instantiateClass(Tag.class);
        ReflectionTestUtils.setField(tag, "id", TAG_ID);
        ReflectionTestUtils.setField(tag, "name", "Puzzle");
        ReflectionTestUtils.setField(tag, "slug", "puzzle");
        return tag;
    }

    private ForumRatingAggregate aggregate(Long id, Double average, Long count) {
        return new ForumRatingAggregate() {
            @Override
            public Long getDiscussionId() {
                return id;
            }

            @Override
            public Double getAverageRating() {
                return average;
            }

            @Override
            public Long getRatingCount() {
                return count;
            }
        };
    }
}
