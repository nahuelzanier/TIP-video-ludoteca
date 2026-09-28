package com.tip_video_ludoteca.games;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, String> {

    Page<Game> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}
