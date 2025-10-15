package com.bytebard.core.api.filters;

import com.bytebard.core.api.config.TokenAuthConfig;
import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

public class TokenAuthFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final TokenAuthConfig config;
    private final AuthContext authContext;
    private final UserRepository userRepository;
    private final HandlerExceptionResolver exceptionResolver;

    public TokenAuthFilter(TokenAuthConfig config, AuthContext authContext, UserRepository userRepository, HandlerExceptionResolver exceptionResolver) {
        this.config = config;
        this.authContext = authContext;
        this.userRepository = userRepository;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        try {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                var token = authorization.replace("Bearer ", "");
                var userId = config.extractUserId(token);
                if (userId != null) {
                    var user = userRepository.findWithRolesById(Long.valueOf(userId));
                    user.ifPresent(value -> authContext.setContextProps(value, token));
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            authContext.clear();
            exceptionResolver.resolveException(request, response, null, e);
            return;
        }
        if (!authContext.isAuthenticated()) {
            authContext.setUnauthorizedToken();
        }
        filterChain.doFilter(request, response);
    }
}
