package com.tip_video_ludoteca.news;

import com.tip_video_ludoteca.news.NoticiaService.NewsPage;
import com.tip_video_ludoteca.news.NoticiaService.NewsResponse;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoticiaServiceTests {

    private static final String EMAIL = "author@test.com";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private NoticiaRepository noticias;

    @Mock
    private NoticiaImageRepository images;

    @Mock
    private UserRepository users;

    private NoticiaService service;

    @BeforeEach
    void setUp() {
        service = new NoticiaService(noticias, images, users);
    }

    @Test
    void createsANewsWithServerAssignedDateAndAuthor() {
        User author = author(1L, "author");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
        when(noticias.save(any(Noticia.class))).thenAnswer(invocation -> {
            Noticia value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 10L);
            return value;
        });

        NewsResponse result = service.create("  Titulo  ", "  Descripcion  ", null, EMAIL);

        ArgumentCaptor<Noticia> saved = ArgumentCaptor.forClass(Noticia.class);
        verify(noticias).save(saved.capture());
        assertThat(saved.getValue().getTitle()).isEqualTo("Titulo");
        assertThat(saved.getValue().getDescription()).isEqualTo("Descripcion");
        assertThat(saved.getValue().getAuthor()).isSameAs(author);
        assertThat(saved.getValue().getPublishedAt()).isNotNull();
        assertThat(result.authorUsername()).isEqualTo("author");
        assertThat(result.imageUrl()).isNull();
    }

    @Test
    void storesAValidOptionalImage() {
        User author = author(1L, "author");
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
        when(noticias.save(any(Noticia.class))).thenAnswer(invocation -> {
            Noticia value = invocation.getArgument(0);
            ReflectionTestUtils.setField(value, "id", 11L);
            return value;
        });
        when(images.findById(11L)).thenReturn(Optional.empty());

        NewsResponse result = service.create(
                "Titulo", "Descripcion",
                new MockMultipartFile("image", "image.png", "image/png", PNG),
                EMAIL);

        verify(images).save(any(NoticiaImage.class));
        assertThat(result.imageUrl()).isEqualTo("/api/news/11/image");
    }

    @Test
    void rejectsBlankTitles() {
        assertThatThrownBy(() -> service.create("  ", "Descripcion", null, EMAIL))
                .isInstanceOf(InvalidNoticiaException.class);

        verify(noticias, never()).save(any(Noticia.class));
    }

    @Test
    void rejectsImagesThatAreTooBig() {
        byte[] tooBig = new byte[NoticiaService.MAX_IMAGE_BYTES + 1];
        System.arraycopy(PNG, 0, tooBig, 0, PNG.length);

        assertThatThrownBy(() -> service.create(
                "Titulo", "Descripcion",
                new MockMultipartFile("image", "big.png", "image/png", tooBig), EMAIL))
                .isInstanceOf(InvalidNoticiaException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    void feedOnlyContainsNewsFromTheAuthenticatedUserAndMapsIt() {
        User author = author(1L, "author");
        Noticia older = new Noticia("Vieja", "Descripcion vieja", author);
        Noticia newer = new Noticia("Nueva", "Descripcion nueva", author);
        ReflectionTestUtils.setField(older, "id", 1L);
        ReflectionTestUtils.setField(newer, "id", 2L);
        PageRequest pageable = PageRequest.of(0, 9);

        when(noticias.findFeedByFollowerEmail(EMAIL, pageable))
                .thenReturn(new PageImpl<>(List.of(newer, older), pageable, 2));

        NewsPage page = service.listFeed(EMAIL, pageable);

        assertThat(page.content()).extracting(NewsResponse::title)
                .containsExactly("Nueva", "Vieja");
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.hasNext()).isFalse();
    }

    @Test
    void onlyTheAuthorCanEditANews() {
        User author = author(1L, "author");
        User other = author(2L, "other");
        Noticia noticia = new Noticia("Titulo", "Descripcion", author);
        ReflectionTestUtils.setField(noticia, "id", 5L);

        when(noticias.findById(5L)).thenReturn(Optional.of(noticia));
        when(users.findByEmailIgnoreCase("other@test.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(5L, "Otro", "Otra", null, "other@test.com"))
                .isInstanceOf(NoticiaEditForbiddenException.class);

        verify(noticias, never()).save(any(Noticia.class));
    }

    @Test
    void deletesANewsAndItsImage() {
        User author = author(1L, "author");
        Noticia noticia = new Noticia("Titulo", "Descripcion", author);
        ReflectionTestUtils.setField(noticia, "id", 8L);
        NoticiaImage image = new NoticiaImage(noticia, "image/png", PNG);

        when(noticias.findById(8L)).thenReturn(Optional.of(noticia));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));
        when(images.findById(8L)).thenReturn(Optional.of(image));

        service.delete(8L, EMAIL);

        verify(images).delete(image);
        verify(noticias).delete(noticia);
    }

    private User author(long id, String username) {
        User user = new User(username, EMAIL, "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
