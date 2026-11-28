package com.tip_video_ludoteca.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    @EntityGraph(attributePaths = "author")
    @Query("""
            SELECT n FROM Noticia n
            WHERE LOWER(n.author.email) = LOWER(:email)
               OR EXISTS (
                SELECT f FROM Follow f
                WHERE f.followed.id = n.author.id
                  AND f.follower.email = :email
            )
            ORDER BY n.publishedAt DESC, n.id DESC
            """)
    Page<Noticia> findFeedByFollowerEmail(
            @Param("email") String email,
            Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Optional<Noticia> findWithAuthorById(Long id);
}
