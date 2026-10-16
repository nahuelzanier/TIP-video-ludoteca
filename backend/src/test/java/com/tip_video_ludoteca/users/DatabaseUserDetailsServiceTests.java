package com.tip_video_ludoteca.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTests {

    private static final String EMAIL = "player@ludarium.test";
    private static final String HASH = "{bcrypt}stored-hash";

    @Mock
    private UserRepository users;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void loadsTheUserByCaseInsensitiveEmailWithUserRole() {
        when(users.findByEmailIgnoreCase("PLAYER@LUDARIUM.TEST"))
                .thenReturn(Optional.of(new User("player", EMAIL, HASH)));

        UserDetails details = userDetailsService.loadUserByUsername("PLAYER@LUDARIUM.TEST");

        assertThat(details.getUsername()).isEqualTo(EMAIL);
        assertThat(details.getPassword()).isEqualTo(HASH);
        assertThat(details.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_USER");
        verify(users).findByEmailIgnoreCase("PLAYER@LUDARIUM.TEST");
    }

    @Test
    void throwsWhenEmailDoesNotExist() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(EMAIL))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Usuario no encontrado");
    }
}
