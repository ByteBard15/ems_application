package com.bytebard.core.api.security;

import com.bytebard.core.api.models.User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private User user;
    private String token;

    public JwtAuthenticationToken(Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
    }

    public JwtAuthenticationToken(User user, String token, Collection<? extends GrantedAuthority> authorities, boolean authenticated) {
        super(authorities);
        setAuthenticated(authenticated);
        this.user = user;
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return user;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
