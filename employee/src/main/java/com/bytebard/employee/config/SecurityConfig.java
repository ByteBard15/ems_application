package com.bytebard.employee.config;

import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.filters.TokenAuthFilter;
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
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("tokenAuthFilter") OncePerRequestFilter filter,
            @Qualifier("authenticationEntryPoint") DelegatedAuthenticationEntryPoint entryPoint,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource source,
            @Qualifier("accessDeniedHandler") JsonAccessDeniedHandler deniedHandler
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
                        .requestMatchers("/actuator/health")
                        .permitAll()

                        .requestMatchers(String.format("%s/**", Routes.DEPARTMENTS))
                        .hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.GET, String.format("%s", Routes.USERS))
                        .hasAnyRole(Role.ADMIN, Role.MANAGER)

                        .requestMatchers(HttpMethod.GET, String.format("%s/**", Routes.USERS))
                        .hasAnyRole(Role.ADMIN, Role.MANAGER, Role.EMPLOYEE)

                        .requestMatchers(HttpMethod.POST, String.format("%s", Routes.USERS))
                        .hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.PUT, String.format("%s/**", Routes.USERS))
                        .hasRole(Role.ADMIN)

                        .requestMatchers(HttpMethod.DELETE, String.format("%s/**", Routes.USERS))
                        .hasRole(Role.ADMIN)

                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(deniedHandler)
                );

        return http.build();
    }
}
