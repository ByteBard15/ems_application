package com.bytebard.core.api.security;

import com.bytebard.core.api.models.User;
import com.bytebard.core.api.repositories.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationTokenProvider {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public JwtAuthenticationTokenProvider(PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public User authenticate(String email, String password) {
        var findUser = userRepository.findByEmail(email);
        if (findUser.isEmpty()) {
            throw new BadCredentialsException("Invalid email or password");
        }
        var user = findUser.get();
        var matches = passwordEncoder.matches(password, user.getPassword());
        if (!matches) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return user;
    }
}
