package com.tip_video_ludoteca.news;

import com.tip_video_ludoteca.media.ImageValidation;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Service
public class NoticiaService {

    public static final int MAX_TITLE_LENGTH = 150;
    public static final int MAX_DESCRIPTION_LENGTH = 3000;
    public static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final NoticiaRepository noticias;
    private final NoticiaImageRepository images;
    private final UserRepository users;

    public NoticiaService(
            NoticiaRepository noticias,
            NoticiaImageRepository images,
            UserRepository users) {
        this.noticias = noticias;
        this.images = images;
        this.users = users;
    }

    @Transactional
    public NewsResponse create(
            String title,
            String description,
            MultipartFile image,
            String userEmail) {

        String cleanTitle = requireText(title, MAX_TITLE_LENGTH, "El título de la noticia es obligatorio.");
        String cleanDescription = requireText(
                description, MAX_DESCRIPTION_LENGTH, "La descripción de la noticia es obligatoria.");
        ImageData imageData = validateImage(image);

        User author = findUser(userEmail);
        Noticia noticia = noticias.save(new Noticia(cleanTitle, cleanDescription, author));

        storeImage(noticia, imageData);

        return toResponse(noticia);
    }

    @Transactional
    public NewsResponse update(
            Long noticiaId,
            String title,
            String description,
            MultipartFile image,
            String userEmail) {

        Noticia noticia = findNoticia(noticiaId);
        User author = findUser(userEmail);

        if (!noticia.isWrittenBy(author)) {
            throw new NoticiaEditForbiddenException();
        }

        noticia.update(
                requireText(title, MAX_TITLE_LENGTH, "El título de la noticia es obligatorio."),
                requireText(description, MAX_DESCRIPTION_LENGTH, "La descripción de la noticia es obligatoria."));

        storeImage(noticia, validateImage(image));

        return toResponse(noticia);
    }

    @Transactional
    public void delete(Long noticiaId, String userEmail) {
        Noticia noticia = findNoticia(noticiaId);
        User author = findUser(userEmail);

        if (!noticia.isWrittenBy(author)) {
            throw new NoticiaEditForbiddenException();
        }

        images.findById(noticiaId).ifPresent(images::delete);
        noticias.delete(noticia);
    }

    @Transactional(readOnly = true)
    public NewsPage listFeed(String userEmail, Pageable pageable) {
        Page<Noticia> page = noticias.findFeedByFollowerEmail(userEmail, pageable);

        return new NewsPage(
                page.getContent().stream().map(this::toResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
    }

    @Transactional(readOnly = true)
    public NewsResponse getDetail(Long noticiaId) {
        return toResponse(findNoticiaWithAuthor(noticiaId));
    }

    @Transactional(readOnly = true)
    public NewsImage getImage(Long noticiaId) {
        Noticia noticia = findNoticia(noticiaId);

        if (!noticia.hasImage()) {
            throw new NoticiaNotFoundException("La noticia no tiene imagen.");
        }

        NoticiaImage image = images.findById(noticiaId)
                .orElseThrow(() -> new NoticiaNotFoundException("La noticia no tiene imagen."));

        return new NewsImage(image.getContentType(), image.getContent());
    }

    private ImageData validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            return null;
        }

        byte[] content;

        try {
            content = image.getBytes();
        } catch (IOException exception) {
            throw new InvalidNoticiaException("No se pudo leer la imagen.");
        }

        if (content.length > MAX_IMAGE_BYTES) {
            throw new InvalidNoticiaException("La imagen no puede superar los 5 MB.");
        }

        String contentType;

        try {
            contentType = ImageValidation.detectContentType(content);
        } catch (ImageValidation.RejectedImageException exception) {
            throw new InvalidNoticiaException(exception.getMessage());
        }

        return new ImageData(contentType, content);
    }

    private void storeImage(Noticia noticia, ImageData imageData) {
        if (imageData == null) {
            return;
        }

        String contentType = imageData.contentType();
        byte[] content = imageData.content();

        images.findById(noticia.getId()).ifPresentOrElse(
                existing -> existing.replaceContent(contentType, content),
                () -> images.save(new NoticiaImage(noticia, contentType, content)));

        noticia.markWithImage();
    }

    private String requireText(String value, int maxLength, String emptyMessage) {
        String clean = value == null ? "" : value.trim();

        if (clean.isEmpty()) {
            throw new InvalidNoticiaException(emptyMessage);
        }

        if (clean.length() > maxLength) {
            throw new InvalidNoticiaException(
                    "El texto no puede superar los " + maxLength + " caracteres.");
        }

        return clean;
    }

    private Noticia findNoticia(Long noticiaId) {
        return noticias.findById(noticiaId).orElseThrow(NoticiaNotFoundException::new);
    }

    private Noticia findNoticiaWithAuthor(Long noticiaId) {
        return noticias.findWithAuthorById(noticiaId)
                .orElseThrow(NoticiaNotFoundException::new);
    }

    private User findUser(String userEmail) {
        return users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);
    }

    private NewsResponse toResponse(Noticia noticia) {
        String imageUrl = noticia.hasImage()
                ? "/api/news/" + noticia.getId() + "/image"
                : null;

        return new NewsResponse(
                noticia.getId(),
                noticia.getTitle(),
                noticia.getDescription(),
                noticia.getPublishedAt(),
                noticia.getAuthor().getUsername(),
                noticia.getAuthor().getProfileImageUrl(),
                imageUrl);
    }

    public record NewsResponse(
            Long id,
            String title,
            String description,
            Instant publishedAt,
            String authorUsername,
            String authorAvatarUrl,
            String imageUrl) {
    }

    public record NewsPage(
            List<NewsResponse> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious) {
    }

    public record NewsImage(String contentType, byte[] content) {
    }

    private record ImageData(String contentType, byte[] content) {
    }
}
