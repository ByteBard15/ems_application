package com.bytebard.core.api.filters;

import com.bytebard.core.api.config.TokenAuthConfig;
import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class TokenAuthFilterTest {
    @Mock
    private TokenAuthConfig config;

    @Mock
    private AuthContext authContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HandlerExceptionResolver exceptionResolver;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private TokenAuthFilter filter;
    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setUp() {
        mocksCloseable = MockitoAnnotations.openMocks(this);
        filter = new TokenAuthFilter(config, authContext, userRepository, exceptionResolver);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocksCloseable != null) {
            mocksCloseable.close();
        }
    }

    @Test
    void whenBearerTokenPresentAndUserFound_thenSetsContextAndContinues() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(config.extractUserId("token123")).thenReturn("10");

        User user = new User();
        when(userRepository.findWithRolesById(10L)).thenReturn(Optional.of(user));
        when(authContext.isAuthenticated()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(authContext).setContextProps(user, "token123");
        verify(filterChain).doFilter(request, response);
        verify(authContext, never()).setUnauthorizedToken();
        verify(exceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    void whenBearerTokenPresentAndNoUserFound_thenSetsUnauthorizedAndContinues() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(config.extractUserId("token123")).thenReturn("20");
        when(userRepository.findWithRolesById(20L)).thenReturn(Optional.empty());

        when(authContext.isAuthenticated()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(authContext).setUnauthorizedToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void whenAuthorizationHeaderMissing_thenMarksUnauthorizedAndContinues() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        when(authContext.isAuthenticated()).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(authContext).setUnauthorizedToken();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void whenConfigThrowsException_thenClearsContextAndResolvesException() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer bad-token");
        when(config.extractUserId("bad-token")).thenThrow(new IllegalStateException("Invalid token"));

        filter.doFilterInternal(request, response, filterChain);

        verify(authContext).clear();
        verify(exceptionResolver).resolveException(eq(request), eq(response), isNull(), any(IllegalStateException.class));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void whenAlreadyAuthenticated_thenSkipsUnauthorizedMark() throws ServletException, IOException {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer token123");
        when(config.extractUserId("token123")).thenReturn("10");
        when(userRepository.findWithRolesById(10L)).thenReturn(Optional.empty());
        when(authContext.isAuthenticated()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        verify(authContext, never()).setUnauthorizedToken();
        verify(filterChain).doFilter(request, response);
    }
}
