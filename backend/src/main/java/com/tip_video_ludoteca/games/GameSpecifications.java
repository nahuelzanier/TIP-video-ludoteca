package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.comments.Comment;
import com.tip_video_ludoteca.tags.GameTagAssignment;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class GameSpecifications {

    private GameSpecifications() {
    }

    public static Specification<Game> withFilters(
            String titleQuery,
            Collection<Long> tagIds,
            Double minimumRating) {

        return (game, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteria.equal(game.get("status"), GameStatus.PUBLISHED));

            if (titleQuery != null && !titleQuery.isBlank()) {
                String term = "%" + titleQuery.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteria.like(criteria.lower(game.get("title")), term));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                for (Long tagId : tagIds.stream().distinct().toList()) {
                    var tagQuery = query.subquery(Integer.class);
                    var assignment = tagQuery.from(GameTagAssignment.class);

                    tagQuery.select(criteria.literal(1));
                    tagQuery.where(
                            criteria.equal(assignment.get("game"), game),
                            criteria.equal(assignment.get("tag").get("id"), tagId)
                    );
                    predicates.add(criteria.exists(tagQuery));
                }
            }

            if (minimumRating != null) {
                var ratingsQuery = query.subquery(Double.class);
                var comment = ratingsQuery.from(Comment.class);

                ratingsQuery.select(criteria.avg(comment.get("rating").as(Double.class)));
                ratingsQuery.where(
                        criteria.equal(comment.get("game"), game),
                        criteria.isNull(comment.get("parentComment")),
                        criteria.isNotNull(comment.get("rating"))
                );
                predicates.add(criteria.greaterThanOrEqualTo(ratingsQuery, minimumRating));
            }

            return criteria.and(predicates.toArray(Predicate[]::new));
        };
    }
}
