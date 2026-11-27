package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.news.NoticiaService;
import com.tip_video_ludoteca.news.NoticiaService.NewsImage;
import com.tip_video_ludoteca.news.NoticiaService.NewsPage;
import com.tip_video_ludoteca.news.NoticiaService.NewsResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/news")
public class NoticiaApiController {

    private final NoticiaService noticias;

    public NoticiaApiController(NoticiaService noticias) {
        this.noticias = noticias;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NewsResponse> create(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails principal) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noticias.create(title, description, image, principal.getUsername()));
    }

    @GetMapping
    public NewsPage list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 20);

        return noticias.listFeed(principal.getUsername(), PageRequest.of(safePage, safeSize));
    }

    @GetMapping("/{noticiaId}")
    public NewsResponse detail(@PathVariable Long noticiaId) {
        return noticias.getDetail(noticiaId);
    }

    @GetMapping("/{noticiaId}/image")
    public ResponseEntity<byte[]> image(@PathVariable Long noticiaId) {
        NewsImage image = noticias.getImage(noticiaId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(image.content());
    }

    @PutMapping(value = "/{noticiaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NewsResponse update(
            @PathVariable Long noticiaId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails principal) {

        return noticias.update(noticiaId, title, description, image, principal.getUsername());
    }

    @DeleteMapping("/{noticiaId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long noticiaId,
            @AuthenticationPrincipal UserDetails principal) {
        noticias.delete(noticiaId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
