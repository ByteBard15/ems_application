package com.bytebard.auth.config;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.exceptions.DefaultExceptionHandler;
import com.bytebard.core.api.filters.TokenAuthFilter;
import com.bytebard.core.api.security.DelegatedAuthenticationEntryPoint;
import com.bytebard.core.api.security.JsonAccessDeniedHandler;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {
                SecurityConfig.class,
                SecurityConfigTest.SecurityConfigTestConfiguration.class,
                JsonAccessDeniedHandler.class,
                DelegatedAuthenticationEntryPoint.class,
                DefaultExceptionHandler.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private List<SecurityFilterChain> securityFilterChains;

    @MockitoBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    private DefaultSecurityFilterChain findDefaultChain() {
        return securityFilterChains.stream()
                .filter(c -> c instanceof DefaultSecurityFilterChain)
                .map(c -> (DefaultSecurityFilterChain) c)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No DefaultSecurityFilterChain found in context"));
    }

    @Test
    void securityFiltersAreDisabled_whenConfigured() {
        DefaultSecurityFilterChain chain = findDefaultChain();
        List<Filter> filters = chain.getFilters();

        assertFalse(filters.stream().anyMatch(f -> f instanceof CsrfFilter));
        assertFalse(filters.stream().anyMatch(f -> f instanceof BasicAuthenticationFilter));
        assertFalse(filters.stream().anyMatch(f -> f instanceof UsernamePasswordAuthenticationFilter));
        assertFalse(filters.stream().anyMatch(f -> f instanceof AnonymousAuthenticationFilter));
    }

    @Test
    void permittedRoutesAreAccessible_withoutAuthentication() throws Exception {
        var loginUri = String.format("%s%s", Routes.AUTH, Routes.LOGIN);
        var changePasswordUri = String.format("%s%s", Routes.AUTH, Routes.CHANGE_PASSWORD);
        mockMvc.perform(MockMvcRequestBuilders.get(loginUri).servletPath(loginUri))
                .andExpect(status().isNotFound());

        mockMvc.perform(MockMvcRequestBuilders.get(changePasswordUri).servletPath(changePasswordUri))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRoute_requiresAuthentication_and_noSessionIsCreated() throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/protected"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MockHttpServletResponse resp = result.getResponse();
        String setCookie = resp.getHeader("Set-Cookie");
        assertTrue(setCookie == null || !setCookie.toLowerCase().contains("jsessionid"));
    }

    @TestConfiguration
    static class SecurityConfigTestConfiguration {
        @Bean("tokenAuthFilter")
        public OncePerRequestFilter authFilter() {
            return new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}

