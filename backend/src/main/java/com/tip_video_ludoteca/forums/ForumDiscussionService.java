package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.media.ImageValidation;
import com.tip_video_ludoteca.tags.Tag;
import com.tip_video_ludoteca.tags.TagRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.tip_video_ludoteca.forums.ForumReplyRepository;

@Service
public class ForumDiscussionService {

    private static final int MAX_TITLE_LENGTH = 120;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;
    private static final int MAX_TAGS = 5;
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;

    private final ForumDiscussionRepository discussions;
    private final GameRepository games;
    private final UserRepository users;
    private final TagRepository tags;
    private final ForumRatingRepository ratings;
    private final ForumReplyRepository replies;

    public ForumDiscussionService(
            ForumDiscussionRepository discussions,
            GameRepository games,
            UserRepository users,
            TagRepository tags,
            ForumRatingRepository ratings,
            ForumReplyRepository replies) {
        this.discussions = discussions;
        this.games = games;
        this.users = users;
        this.tags = tags;
        this.ratings = ratings;
        this.replies = replies;
    }

    @Transactional
    public CreatedDiscussion create(
            String gameId,
            String title,
            String description,
            MultipartFile cover,
            List<Long> tagIds,
            String userEmail) {

        String cleanTitle = title == null ? "" : title.trim();
        String cleanDescription = description == null ? "" : description.trim();

        if (cleanTitle.isBlank() || cleanTitle.length() > MAX_TITLE_LENGTH) {
            throw badRequest("El título debe tener entre 1 y 120 caracteres.");
        }

        if (cleanDescription.isBlank()
                || cleanDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw badRequest("La descripción debe tener entre 1 y 2000 caracteres.");
        }

        Game game = games.findById(gameId)
                .filter(found -> found.getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró el juego publicado."
                ));

        User author = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No se encontró al usuario autenticado."
                ));

        ImageData image = readCover(cover);
        Set<Tag> selectedTags = findTags(tagIds);

        ForumDiscussion discussion = new ForumDiscussion(
                game,
                author,
                cleanTitle,
                cleanDescription,
                image.content(),
                image.contentType()
        );

        discussion.getTags().addAll(selectedTags);
        ForumDiscussion saved = discussions.save(discussion);

        return new CreatedDiscussion(saved.getId(), saved.getTitle());
    }

    private Set<Tag> findTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            throw badRequest("Elegí al menos un tag.");
        }

        if (tagIds.size() > MAX_TAGS) {
            throw badRequest("Una discusión puede tener como máximo 5 tags.");
        }

        Set<Long> uniqueIds = new HashSet<>(tagIds);

        if (uniqueIds.contains(null) || uniqueIds.size() != tagIds.size()) {
            throw badRequest("La selección de tags no es válida.");
        }

        List<Tag> selectedTags = tags.findAllById(uniqueIds);

        if (selectedTags.size() != uniqueIds.size()) {
            throw badRequest("Uno o más tags no existen.");
        }

        return new HashSet<>(selectedTags);
    }

    private ImageData readCover(MultipartFile cover) {
        if (cover == null || cover.isEmpty()) {
            throw badRequest("Elegí una imagen para la discusión.");
        }

        if (cover.getSize() > MAX_COVER_BYTES) {
            throw badRequest("La imagen no puede superar los 5 MB.");
        }

        try {
            byte[] content = cover.getBytes();
            String contentType = ImageValidation.detectContentType(content);

            return new ImageData(content, contentType);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo leer la imagen.",
                    exception
            );
        } catch (ImageValidation.RejectedImageException exception) {
            throw badRequest("La imagen debe ser PNG o JPEG.");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record ImageData(byte[] content, String contentType) {
    }

    public record CreatedDiscussion(Long id, String title) {
    }

    @Transactional(readOnly = true)
    public DiscussionPage listPublished(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 24);

        Page<ForumDiscussion> result = discussions.findByGame_Status(
                GameStatus.PUBLISHED,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        List<Long> discussionIds = result.getContent().stream()
                .map(ForumDiscussion::getId)
                .toList();

        Map<Long, ForumRatingAggregate> ratingByDiscussion = new HashMap<>();

        if (!discussionIds.isEmpty()) {
            ratings.aggregateByDiscussionIds(discussionIds)
                    .forEach(rating ->
                            ratingByDiscussion.put(rating.getDiscussionId(), rating));
        }

    List<DiscussionSummary> content = result.getContent().stream()
            .map(discussion -> {
                ForumRatingAggregate aggregate =
                        ratingByDiscussion.get(discussion.getId());

                List<DiscussionTag> discussionTags = discussion.getTags().stream()
                        .map(tag -> new DiscussionTag(
                                tag.getId(),
                                tag.getName(),
                                tag.getSlug()
                        ))
                        .toList();

                return new DiscussionSummary(
                        discussion.getId(),
                        discussion.getTitle(),
                        discussion.getDescription(),
                        discussion.getCreatedAt(),
                        discussion.getGame().getId(),
                        discussion.getGame().getTitle(),
                        discussion.getAuthor().getUsername(),
                        discussion.getAuthor().getProfileImageUrl(),
                        "/api/forum/discussions/"
                                + discussion.getId()
                                + "/cover",
                        discussionTags,
                        aggregate == null ? null : aggregate.getAverageRating(),
                        aggregate == null ? 0 : aggregate.getRatingCount()
                );
            })
            .toList();

        return new DiscussionPage(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    @Transactional(readOnly = true)
    public CoverImage getPublishedCover(Long discussionId) {
        ForumDiscussion discussion = discussions.findById(discussionId)
                .filter(found -> found.getGame().getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la discusión."
                ));

        return new CoverImage(
                discussion.getCoverImageContentType(),
                discussion.getCoverImage()
        );
    }

    public record DiscussionTag(Long id, String name, String slug) {
    }

    public record DiscussionSummary(
            Long id,
            String title,
            String description,
            Instant createdAt,
            String gameId,
            String gameTitle,
            String authorUsername,
            String authorAvatarUrl,
            String coverImageUrl,
            List<DiscussionTag> tags,
            Double averageRating,
            long ratingCount) {
    }

    public record DiscussionPage(
            List<DiscussionSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious) {
    }

    public record CoverImage(String contentType, byte[] content) {
    }

    @Transactional(readOnly = true)
    public DiscussionDetail getPublishedDetail(
            Long discussionId,
            String userEmail) {

        ForumDiscussion discussion = discussions.findWithDetailsById(discussionId)
                .filter(found ->
                        found.getGame().getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la discusión."
                ));

        ForumRatingAggregate aggregate = ratings
                .aggregateByDiscussionIds(List.of(discussionId))
                .stream()
                .findFirst()
                .orElse(null);

        Integer myRating = userEmail == null
                ? null
                : users.findByEmailIgnoreCase(userEmail)
                        .flatMap(user -> ratings.findByDiscussion_IdAndUser_Id(
                                discussionId,
                                user.getId()))
                        .map(rating -> rating.getRating().intValue())
                        .orElse(null);

        List<DiscussionTag> discussionTags = discussion.getTags().stream()
                .map(tag -> new DiscussionTag(
                        tag.getId(),
                        tag.getName(),
                        tag.getSlug()))
                .toList();

        return new DiscussionDetail(
                discussion.getId(),
                discussion.getTitle(),
                discussion.getDescription(),
                discussion.getCreatedAt(),
                discussion.getGame().getId(),
                discussion.getGame().getTitle(),
                discussion.getAuthor().getUsername(),
                discussion.getAuthor().getProfileImageUrl(),
                "/api/forum/discussions/" + discussion.getId() + "/cover",
                discussionTags,
                aggregate == null ? null : aggregate.getAverageRating(),
                aggregate == null ? 0 : aggregate.getRatingCount(),
                myRating
        );
    }

    public record DiscussionDetail(
            Long id,
            String title,
            String description,
            Instant createdAt,
            String gameId,
            String gameTitle,
            String authorUsername,
            String authorAvatarUrl,
            String coverImageUrl,
            List<DiscussionTag> tags,
            Double averageRating,
            long ratingCount,
            Integer myRating) {
    }

        @Transactional(readOnly = true)
    public List<ForumReplyResponse> listReplies(Long discussionId) {
        findPublishedDiscussion(discussionId);

        return replies
                .findByDiscussion_IdOrderByCreatedAtAscIdAsc(discussionId)
                .stream()
                .map(reply -> new ForumReplyResponse(
                        reply.getId(),
                        reply.getContent(),
                        reply.getCreatedAt(),
                        reply.getAuthor().getUsername(),
                        reply.getAuthor().getProfileImageUrl(),
                        reply.getParentReply() == null
                                ? null
                                : reply.getParentReply().getId()
                ))
                .toList();
    }

    @Transactional
    public ForumReplyResponse createReply(
            Long discussionId,
            CreateForumReplyRequest request,
            String userEmail) {

        ForumDiscussion discussion = findPublishedDiscussion(discussionId);

        if (userEmail == null || userEmail.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Debes iniciar sesión para responder."
            );
        }

        User author = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No se encontró al usuario autenticado."
                ));

        String content = request == null || request.content() == null
                ? ""
                : request.content().trim();

        if (content.isBlank() || content.length() > 3000) {
            throw badRequest("La respuesta debe tener entre 1 y 3000 caracteres.");
        }

        ForumReply parent = null;

        if (request.parentReplyId() != null) {
            parent = replies
                    .findByIdAndDiscussion_Id(
                            request.parentReplyId(),
                            discussionId
                    )
                    .orElseThrow(() -> badRequest(
                            "La respuesta que querés responder no pertenece a esta discusión."
                    ));
        }

        ForumReply saved = replies.save(
                new ForumReply(discussion, author, parent, content)
        );

        return new ForumReplyResponse(
                saved.getId(),
                saved.getContent(),
                saved.getCreatedAt(),
                author.getUsername(),
                author.getProfileImageUrl(),
                parent == null ? null : parent.getId()
        );
    }

    private ForumDiscussion findPublishedDiscussion(Long discussionId) {
        return discussions.findById(discussionId)
                .filter(discussion ->
                        discussion.getGame().getStatus() == GameStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró la discusión."
                ));
    }

    public record ForumReplyResponse(
            Long id,
            String content,
            Instant createdAt,
            String authorUsername,
            String authorAvatarUrl,
            Long parentReplyId
    ) {
    }

        @Transactional
    public RatingResult rateDiscussion(
            Long discussionId,
            Integer score,
            String userEmail) {

        ForumDiscussion discussion = findPublishedDiscussion(discussionId);

        if (score == null || score < 1 || score > 5) {
            throw badRequest("La valoración debe estar entre 1 y 5.");
        }

        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "No se encontró al usuario autenticado."
                ));

        ForumRating rating = ratings
                .findByDiscussion_IdAndUser_Id(discussionId, user.getId())
                .orElse(null);

        if (rating == null) {
            rating = new ForumRating(discussion, user, score);
        } else {
            rating.updateRating(score);
        }

        ratings.saveAndFlush(rating);

        ForumRatingAggregate aggregate = ratings
                .aggregateByDiscussionIds(List.of(discussionId))
                .stream()
                .findFirst()
                .orElseThrow();

        return new RatingResult(
                discussionId,
                score,
                aggregate.getAverageRating(),
                aggregate.getRatingCount()
        );
    }

    public record RatingResult(
            Long discussionId,
            Integer myRating,
            Double averageRating,
            Long ratingCount
    ) {
    }
    
}