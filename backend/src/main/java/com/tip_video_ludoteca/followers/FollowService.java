package com.tip_video_ludoteca.followers;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FollowService {

    private final FollowRepository follows;
    private final UserRepository users;

    public FollowService(FollowRepository follows, UserRepository users) {
        this.follows = follows;
        this.users = users;
    }

    @Transactional
    public Follow follow(String username, String userEmail) {
        User follower = findUserByEmail(userEmail);
        User followed = findUserByUsername(username);

        if (follower.getId().equals(followed.getId())) {
            throw new InvalidFollowException("No podés seguirte a vos mismo.");
        }

        return follows.findByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())
                .orElseGet(() -> follows.save(new Follow(follower, followed)));
    }

    @Transactional
    public void unfollow(String username, String userEmail) {
        User follower = findUserByEmail(userEmail);
        User followed = findUserByUsername(username);

        follows.findByFollower_IdAndFollowed_Id(follower.getId(), followed.getId())
                .ifPresent(follows::delete);
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(String username, String userEmail) {
        return follows.existsByFollower_EmailIgnoreCaseAndFollowed_UsernameIgnoreCase(
                userEmail, username);
    }

    @Transactional(readOnly = true)
    public FollowCounts counts(String username) {
        User user = findUserByUsername(username);

        return new FollowCounts(
                username,
                follows.countByFollowed_Id(user.getId()),
                follows.countByFollower_Id(user.getId()));
    }

    private User findUserByEmail(String userEmail) {
        return users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);
    }

    private User findUserByUsername(String username) {
        return users.findByUsernameIgnoreCase(username)
                .orElseThrow(UserNotFoundException::new);
    }

    public record FollowCounts(String username, long followersCount, long followingCount) {
    }
}
