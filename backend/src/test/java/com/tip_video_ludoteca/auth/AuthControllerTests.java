package com.tip_video_ludoteca.auth;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {

    private static final String EMAIL = "luqui@example.test";

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SecurityContextRepository securityContextRepository;

    @Mock
    private SessionAuthenticationStrategy sessionAuthenticationStrategy;

    @InjectMocks
    private AuthController auth;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerNormalizesUsernameAndEmailAndStoresAnEncodedPassword() {
        when(users.existsByUsernameIgnoreCase("luqui")).thenReturn(false);
        when(users.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("{bcrypt}encoded");
        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 11L);
            return saved;
        });

        var response = auth.register(new AuthController.RegisterRequest(
                " LuQui ", " LUQUI@EXAMPLE.TEST ", "secret123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(new AuthController.UserResponse(
                11L, "luqui", EMAIL));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("{bcrypt}encoded");
    }

    @Test
    void registerRejectsAnExistingUsernameWithoutSaving() {
        when(users.existsByUsernameIgnoreCase("luqui")).thenReturn(true);

        var response = auth.register(new AuthController.RegisterRequest(
                "Luqui", EMAIL, "secret123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(users, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void loginChangesSessionIdAndSavesTheAuthenticatedSecurityContext() {
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                EMAIL, null, List.of());
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthController.UserResponse loggedIn = auth.login(
                new AuthController.LoginRequest(" LUQUI@EXAMPLE.TEST ", "secret123"),
                request,
                response);

        assertThat(loggedIn).isEqualTo(new AuthController.UserResponse(11L, "luqui", EMAIL));
        verify(sessionAuthenticationStrategy).onAuthentication(authentication, request, response);
        ArgumentCaptor<SecurityContext> contextCaptor = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(
                contextCaptor.capture(), eq(request), eq(response));
        assertThat(contextCaptor.getValue().getAuthentication()).isSameAs(authentication);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test
    void invalidCredentialsReturnAnUnauthorizedJsonResponse() {
        var response = auth.invalidCredentials(new BadCredentialsException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).containsEntry("error", "Email o contraseña incorrectos.");
    }

    private User user() {
        User user = new User("luqui", EMAIL, "{bcrypt}encoded");
        ReflectionTestUtils.setField(user, "id", 11L);
        return user;
    }
}
