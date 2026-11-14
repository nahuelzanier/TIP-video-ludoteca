package com.tip_video_ludoteca.games;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameCustomizationRepository extends JpaRepository<GameCustomization, String> {
    Optional<GameCustomization> findByGameId(String gameId);
}
