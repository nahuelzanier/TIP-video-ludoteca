package com.tip_video_ludoteca.forums;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ForumTagRepository extends JpaRepository<ForumTag, Long> {

    List<ForumTag> findAllByOrderByNameAsc();
}