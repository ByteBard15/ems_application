package com.bytebard.auth.service;

import com.bytebard.auth.types.ChangePasswordRequest;
import com.bytebard.auth.types.LoginRequest;
import com.bytebard.core.api.config.TokenAuthConfig;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.core.api.security.JwtAuthenticationTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private JwtAuthenticationTokenProvider provider;

    @Mock
    private TokenAuthConfig config;

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder encoder;

    private AuthService authService;
    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setup() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        encoder = new BCryptPasswordEncoder();
        authService = new AuthService(provider, config, encoder, userRepository);
        ReflectionTestUtils.setField(authService, "accessTokenExpiryInHours", 2L);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    @Test
    void login_fails_whenUserInactive() {
        var user = new User();
        user.setId(1L);
        user.setStatus(Status.INACTIVE);

        when(provider.authenticate("test@mail.com", "pass")).thenReturn(user);

        var req = new LoginRequest("test@mail.com", "pass");

        var ex = assertThrows(HttpClientErrorException.class, () -> authService.login(req));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }

    @Test
    void login_returnsToken_whenUserActive() {
        var user = new User();
        user.setId(1L);
        user.setStatus(Status.ACTIVE);

        when(provider.authenticate("test@mail.com", "pass")).thenReturn(user);
        when(config.token(eq(1L), anyLong())).thenReturn("jwt-token");

        var req = new LoginRequest("test@mail.com", "pass");
        var resp = authService.login(req);

        assertEquals("jwt-token", resp.getToken());
        verify(config).token(eq(1L), anyLong());
    }

    @Test
    void changePassword_encodesPassword_andActivatesUser() {
        var user = new User();
        user.setId(1L);
        user.setStatus(Status.INACTIVE);
        user.setPassword("old");

        when(provider.authenticate("test@mail.com", "oldPass")).thenReturn(user);

        var req = new ChangePasswordRequest("test@mail.com", "oldPass", "TestPass11@");
        authService.changePassword(req);

        assertTrue(encoder.matches("TestPass11@", user.getPassword()));
        assertEquals(Status.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_fails_whenPasswordInvalid() {
        var req = new ChangePasswordRequest("test@mail.com", "old", "bad");

        var ex = assertThrows(HttpClientErrorException.class, () -> authService.changePassword(req));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }
}

