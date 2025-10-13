package com.bytebard.employee.config;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.filters.JwtAuthFilter;
import com.bytebard.core.api.models.Role;
import com.bytebard.core.api.security.DelegatedAuthenticationEntryPoint;
import com.bytebard.core.api.security.JsonAccessDeniedHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter filter,
            DelegatedAuthenticationEntryPoint entryPoint,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource source,
            JsonAccessDeniedHandler deniedHandler
    ) throws Exception {
        http.httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .anonymous(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(customizer -> customizer.configurationSource(source))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.NEVER))
                .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(String.format("%s%s/**", Routes.API_V1, Routes.DEPARTMENTS)).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.POST, String.format("%s%s", Routes.API_V1, Routes.USERS)).hasRole(Role.ADMIN)
                        .requestMatchers(HttpMethod.PUT, String.format("%s%s", Routes.API_V1, Routes.USERS)).hasRole(Role.ADMIN)
                        .anyRequest()
                        .authenticated()
                ).exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                );

        return http.build();
    }
}
