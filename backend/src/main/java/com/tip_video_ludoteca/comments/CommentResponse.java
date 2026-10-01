package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.users.User;

import java.time.Instant;
import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        Integer rating,
        Instant createdAt,
        String username,
        String profileImageUrl,
        boolean isGameAuthor,
        List<CommentResponse> replies) {

    static CommentResponse from(Comment comment, Game game, boolean isGameAuthor) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getRating(),
                comment.getCreatedAt(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getProfileImageUrl(),
                isGameAuthor,
                List.of());
    }

    CommentResponse withReplies(List<CommentResponse> replies) {
        return new CommentResponse(
                id,
                content,
                rating,
                createdAt,
                username,
                profileImageUrl,
                isGameAuthor,
                replies);
    }

    static boolean isGameAuthor(Comment comment, Game game) {
        User author = comment.getAuthor();
        User gameAuthor = game.getOwner();

        return gameAuthor != null && author.getId().equals(gameAuthor.getId());
    }
}