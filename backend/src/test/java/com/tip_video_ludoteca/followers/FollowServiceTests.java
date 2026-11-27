package com.tip_video_ludoteca.followers;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTests {

    private static final String FOLLOWER_EMAIL = "follower@test.com";
    private static final String FOLLOWED_USERNAME = "creator";

    @Mock
    private FollowRepository follows;

    @Mock
    private UserRepository users;

    private FollowService followService;

    @BeforeEach
    void setUp() {
        followService = new FollowService(follows, users);
    }

    @Test
    void followsAnotherUser() {
        User follower = user(1L, "follower", FOLLOWER_EMAIL);
        User followed = user(2L, FOLLOWED_USERNAME, "creator@test.com");

        givenUsers(follower, followed);
        when(follows.findByFollower_IdAndFollowed_Id(1L, 2L)).thenReturn(Optional.empty());
        when(follows.save(any(Follow.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Follow result = followService.follow(FOLLOWED_USERNAME, FOLLOWER_EMAIL);

        assertThat(result.getId().getFollowerId()).isEqualTo(1L);
        assertThat(result.getId().getFollowedId()).isEqualTo(2L);
        verify(follows).save(any(Follow.class));
    }

    @Test
    void doesNotDuplicateAFollow() {
        User follower = user(1L, "follower", FOLLOWER_EMAIL);
        User followed = user(2L, FOLLOWED_USERNAME, "creator@test.com");
        Follow existing = new Follow(follower, followed);

        givenUsers(follower, followed);
        when(follows.findByFollower_IdAndFollowed_Id(1L, 2L)).thenReturn(Optional.of(existing));

        Follow result = followService.follow(FOLLOWED_USERNAME, FOLLOWER_EMAIL);

        assertThat(result).isSameAs(existing);
        verify(follows, never()).save(any(Follow.class));
    }

    @Test
    void doesNotAllowFollowingYourself() {
        User user = user(1L, FOLLOWED_USERNAME, FOLLOWER_EMAIL);

        givenUsers(user, user);

        assertThatThrownBy(() -> followService.follow(FOLLOWED_USERNAME, FOLLOWER_EMAIL))
                .isInstanceOf(InvalidFollowException.class)
                .hasMessageContaining("No podés seguirte");

        verify(follows, never()).save(any(Follow.class));
    }

    @Test
    void unfollowsAnExistingRelation() {
        User follower = user(1L, "follower", FOLLOWER_EMAIL);
        User followed = user(2L, FOLLOWED_USERNAME, "creator@test.com");
        Follow existing = new Follow(follower, followed);

        givenUsers(follower, followed);
        when(follows.findByFollower_IdAndFollowed_Id(1L, 2L)).thenReturn(Optional.of(existing));

        followService.unfollow(FOLLOWED_USERNAME, FOLLOWER_EMAIL);

        verify(follows).delete(existing);
    }

    @Test
    void unfollowIsIdempotent() {
        User follower = user(1L, "follower", FOLLOWER_EMAIL);
        User followed = user(2L, FOLLOWED_USERNAME, "creator@test.com");

        givenUsers(follower, followed);
        when(follows.findByFollower_IdAndFollowed_Id(1L, 2L)).thenReturn(Optional.empty());

        followService.unfollow(FOLLOWED_USERNAME, FOLLOWER_EMAIL);

        verify(follows, never()).delete(any(Follow.class));
    }

    @Test
    void reportsFollowStatusAndCounts() {
        when(follows.existsByFollower_EmailIgnoreCaseAndFollowed_UsernameIgnoreCase(
                FOLLOWER_EMAIL, FOLLOWED_USERNAME)).thenReturn(true);

        User followed = user(2L, FOLLOWED_USERNAME, "creator@test.com");
        when(users.findByUsernameIgnoreCase(FOLLOWED_USERNAME)).thenReturn(Optional.of(followed));
        when(follows.countByFollowed_Id(2L)).thenReturn(7L);
        when(follows.countByFollower_Id(2L)).thenReturn(3L);

        assertThat(followService.isFollowing(FOLLOWED_USERNAME, FOLLOWER_EMAIL)).isTrue();
        assertThat(followService.counts(FOLLOWED_USERNAME))
                .extracting(
                        FollowService.FollowCounts::followersCount,
                        FollowService.FollowCounts::followingCount)
                .containsExactly(7L, 3L);
    }

    @Test
    void failsWhenTheFollowedUserDoesNotExist() {
        when(users.findByEmailIgnoreCase(FOLLOWER_EMAIL))
                .thenReturn(Optional.of(user(1L, "follower", FOLLOWER_EMAIL)));
        when(users.findByUsernameIgnoreCase(FOLLOWED_USERNAME)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> followService.follow(FOLLOWED_USERNAME, FOLLOWER_EMAIL))
                .isInstanceOf(UserNotFoundException.class);
    }

    private void givenUsers(User follower, User followed) {
        when(users.findByEmailIgnoreCase(FOLLOWER_EMAIL)).thenReturn(Optional.of(follower));
        when(users.findByUsernameIgnoreCase(FOLLOWED_USERNAME)).thenReturn(Optional.of(followed));
    }

    private User user(long id, String username, String email) {
        User user = new User(username, email, "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
