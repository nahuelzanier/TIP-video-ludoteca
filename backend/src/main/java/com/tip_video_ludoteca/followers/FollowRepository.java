package com.tip_video_ludoteca.followers;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    Optional<Follow> findByFollower_IdAndFollowed_Id(Long followerId, Long followedId);

    boolean existsByFollower_EmailIgnoreCaseAndFollowed_UsernameIgnoreCase(
            String followerEmail, String followedUsername);

    long countByFollowed_Id(Long followedId);

    long countByFollower_Id(Long followerId);
}
