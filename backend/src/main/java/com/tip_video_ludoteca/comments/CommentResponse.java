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
        boolean edited,
        String username,
        String profileImageUrl,
        boolean isGameAuthor,
        boolean isTargetAuthor,
        List<CommentResponse> replies) {

    public CommentResponse(
            Long id,
            String content,
            Integer rating,
            Instant createdAt,
            boolean edited,
            String username,
            String profileImageUrl,
            boolean isGameAuthor,
            List<CommentResponse> replies) {
        this(id, content, rating, createdAt, edited, username, profileImageUrl,
                isGameAuthor, isGameAuthor, replies);
    }

    static CommentResponse from(Comment comment, Game game, boolean isGameAuthor) {
        return from(comment, isGameAuthor, isGameAuthor);
    }

    static CommentResponse from(
            Comment comment,
            boolean isGameAuthor,
            boolean isTargetAuthor) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getRating(),
                comment.getCreatedAt(),
                comment.isEdited(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getProfileImageUrl(),
                isGameAuthor,
                isTargetAuthor,
                List.of());
    }

    CommentResponse withReplies(List<CommentResponse> replies) {
        return new CommentResponse(
                id,
                content,
                rating,
                createdAt,
                edited,
                username,
                profileImageUrl,
                isGameAuthor,
                isTargetAuthor,
                replies);
    }

    static boolean isGameAuthor(Comment comment, Game game) {
        User author = comment.getAuthor();
        User gameAuthor = game.getOwner();

        return gameAuthor != null && author.getId().equals(gameAuthor.getId());
    }
}
