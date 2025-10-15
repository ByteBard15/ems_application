package com.bytebard.core.api.security;

import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtAuthenticationTokenProviderTest {

    private PasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private JwtAuthenticationTokenProvider provider;

    @BeforeEach
    void setUp() {
        passwordEncoder = mock(PasswordEncoder.class);
        userRepository = mock(UserRepository.class);
        provider = new JwtAuthenticationTokenProvider(passwordEncoder, userRepository);
    }

    @Test
    void authenticate_returnsUser_whenCredentialsValid() {
        var user = new User();
        user.setEmail("test@mail.com");
        user.setPassword("encodedPass");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rawPass", "encodedPass")).thenReturn(true);

        var result = provider.authenticate("test@mail.com", "rawPass");

        assertNotNull(result);
        assertEquals(user, result);
        verify(userRepository).findByEmail("test@mail.com");
        verify(passwordEncoder).matches("rawPass", "encodedPass");
    }

    @Test
    void authenticate_throwsBadCredentialsException_whenUserNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        var ex = assertThrows(BadCredentialsException.class, () ->
                provider.authenticate("unknown@mail.com", "somePass")
        );

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail("unknown@mail.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void authenticate_throwsBadCredentialsException_whenPasswordDoesNotMatch() {
        var user = new User();
        user.setPassword("encodedPass");

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", "encodedPass")).thenReturn(false);

        var ex = assertThrows(BadCredentialsException.class, () ->
                provider.authenticate("test@mail.com", "wrongPass")
        );

        assertEquals("Invalid email or password", ex.getMessage());
        verify(userRepository).findByEmail("test@mail.com");
        verify(passwordEncoder).matches("wrongPass", "encodedPass");
    }
}
