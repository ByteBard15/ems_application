package com.bytebard.core.api.context;

import com.bytebard.core.api.models.User;
import com.bytebard.core.api.security.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AuthContext {
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    public boolean isAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return authentication.isAuthenticated();
        }
        return false;
    }

    public void clear() {
        SecurityContextHolder.clearContext();
    }

    public void setContextProps(User user, String token) {
        List<SimpleGrantedAuthority> roles = user.getRoles()
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList();
        var authentication = new JwtAuthenticationToken(user, token, roles, true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public void setUnauthorizedToken() {
        var authentication = new JwtAuthenticationToken(null, null, List.of(), false);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public String getToken() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return authentication.getCredentials().toString();
        }
        return null;
    }
}
