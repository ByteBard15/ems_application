package com.bytebard.employee.config;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.context.AuthContext;
import com.bytebard.core.api.exceptions.DefaultExceptionHandler;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.models.User;
import com.bytebard.core.api.security.DelegatedAuthenticationEntryPoint;
import com.bytebard.core.api.security.JsonAccessDeniedHandler;
import com.bytebard.utils.DateUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.context.SecurityContextHolder;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        classes = {
                SecurityConfig.class,
                SecurityConfigTest.SecurityConfigTestConfiguration.class,
                JsonAccessDeniedHandler.class,
                DelegatedAuthenticationEntryPoint.class,
                DefaultExceptionHandler.class,
                AuthContext.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=" // clear config imports to avoid configserver: attempts
})
@ImportAutoConfiguration({
        WebMvcAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthContext authContext;

    @Autowired
    private List<SecurityFilterChain> securityFilterChains;

    @MockitoBean(name = "corsConfigurationSource")
    private CorsConfigurationSource corsConfigurationSource;

    private RequestPostProcessor authUser(User user) {
        return request -> {
            authContext.setContextProps(user, "token123");
            return request;
        };
    }

    private DefaultSecurityFilterChain findDefaultChain() {
        return securityFilterChains.stream()
                .filter(c -> c instanceof DefaultSecurityFilterChain)
                .map(c -> (DefaultSecurityFilterChain) c)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No DefaultSecurityFilterChain found in context"));
    }

    @BeforeEach
    void setup() {
        authContext.clear();
    }

    @Test
    void securityFiltersAreDisabled_whenConfigured() {
        DefaultSecurityFilterChain chain = findDefaultChain();
        List<Filter> filters = chain.getFilters();

        assertFalse(filters.stream().anyMatch(f -> f instanceof CsrfFilter), "CsrfFilter should be disabled");
        assertFalse(filters.stream().anyMatch(f -> f instanceof BasicAuthenticationFilter), "BasicAuthenticationFilter should be disabled");
        assertFalse(filters.stream().anyMatch(f -> f instanceof UsernamePasswordAuthenticationFilter), "UsernamePasswordAuthenticationFilter should be disabled");
        assertFalse(filters.stream().anyMatch(f -> f instanceof AnonymousAuthenticationFilter), "AnonymousAuthenticationFilter should be disabled");
    }

    @Test
    void usersEndpoints_requireAdminRole_forCreateAndUpdate() throws Exception {
        var userUrl = String.format("%s%s", Routes.API_V1, Routes.USERS);
        var user = new User("Admin", "User", "pass", "admin@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.ADMIN)));

        mockMvc.perform(post(userUrl).with(authUser(user))
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\",\"lastName\":\"A\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(userUrl + "/1").with(authUser(user))
                        .contentType("application/json")
                        .content("{\"firstName\":\"Updated\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void usersEndpoints_allowManagerToViewOnly() throws Exception {
        var userUrl = String.format("%s%s", Routes.API_V1, Routes.USERS);
        var user = new User("Manager", "User", "pass", "manager@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.MANAGER)));
        authContext.setContextProps(user, "token123");

        mockMvc.perform(get(userUrl))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(userUrl + "/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(userUrl)
                        .contentType("application/json")
                        .content("{\"firstName\":\"Alice\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usersEndpoints_denyEmployeeAccessToAll() throws Exception {
        var userUrl = String.format("%s%s", Routes.API_V1, Routes.USERS);
        var user = new User("Employee", "User", "pass", "employee@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.EMPLOYEE)));
        authContext.setContextProps(user, "token123");

        mockMvc.perform(get(userUrl))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get(userUrl + "/1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(userUrl)
                        .contentType("application/json")
                        .content("{\"firstName\":\"Bob\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void departmentsEndpoints_requireAdminRole_forAllOperations() throws Exception {
        var deptUrl = String.format("%s%s", Routes.API_V1, Routes.DEPARTMENTS);
        var user = new User("Admin", "User", "pass", "admin@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.ADMIN)));
        authContext.setContextProps(user, "token123");

        mockMvc.perform(get(deptUrl))
                .andExpect(status().isNotFound());

        mockMvc.perform(post(deptUrl)
                        .contentType("application/json")
                        .content("{\"name\":\"Finance\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(deptUrl + "/1")
                        .contentType("application/json")
                        .content("{\"name\":\"Finance Updated\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(deptUrl + "/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void departmentsEndpoints_denyManagerAccess() throws Exception {
        var deptUrl = String.format("%s%s", Routes.API_V1, Routes.DEPARTMENTS);
        var user = new User("Manager", "User", "pass", "manager@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.MANAGER)));
        authContext.setContextProps(user, "token123");

        mockMvc.perform(get(deptUrl))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(deptUrl)
                        .contentType("application/json")
                        .content("{\"name\":\"HR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void departmentsEndpoints_denyEmployeeAccess() throws Exception {
        var deptUrl = String.format("%s%s", Routes.API_V1, Routes.DEPARTMENTS);
        var user = new User("Employee", "User", "pass", "emp@mail.com", Status.ACTIVE, DateUtils.now(), Set.of(new Role(Role.EMPLOYEE)));
        authContext.setContextProps(user, "token123");

        mockMvc.perform(get(deptUrl))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post(deptUrl)
                        .contentType("application/json")
                        .content("{\"name\":\"Ops\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedRoute_requiresAuthentication_and_noSessionIsCreated() throws Exception {
        MvcResult result = mockMvc.perform(get("/protected"))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MockHttpServletResponse resp = result.getResponse();
        String setCookie = resp.getHeader("Set-Cookie");

        assertTrue(setCookie == null || !setCookie.toLowerCase().contains("jsessionid"),
                "No JSESSIONID cookie should be set when session creation policy is NEVER");
    }

    @TestConfiguration
    static class SecurityConfigTestConfiguration {

        @Bean("tokenAuthFilter")
        public OncePerRequestFilter authFilter() {
            return new OncePerRequestFilter() {
                @Override
                protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain
                ) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }
}
