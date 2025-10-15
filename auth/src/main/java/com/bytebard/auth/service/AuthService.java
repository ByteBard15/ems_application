package com.bytebard.auth.service;

import com.bytebard.auth.types.ChangePasswordRequest;
import com.bytebard.auth.types.LoginRequest;
import com.bytebard.auth.types.LoginResponse;
import com.bytebard.core.api.config.TokenAuthConfig;
import com.bytebard.core.api.mappers.UserMapper;
import com.bytebard.core.api.models.Status;
import com.bytebard.core.api.repositories.UserRepository;
import com.bytebard.core.api.security.JwtAuthenticationTokenProvider;
import com.bytebard.core.api.validators.FieldValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Objects;

@Service
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${spring.jwt.expiry-in-hours}")
    private Long accessTokenExpiryInHours;

    private final JwtAuthenticationTokenProvider provider;
    private final TokenAuthConfig config;

    public AuthService(JwtAuthenticationTokenProvider provider, TokenAuthConfig config, PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.provider = provider;
        this.config = config;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        var user = this.provider.authenticate(request.getEmail(), request.getPassword());
        if (Objects.equals(user.getStatus(), Status.INACTIVE)) {
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "This user is currently inactive, Please change your password and try again.");
        }
        var token = config.token(user.getId(), accessTokenExpiryInHours);
        return new LoginResponse(token, UserMapper.toUserDTO(user));
    }

    public void changePassword(ChangePasswordRequest request) {
        if (!FieldValidator.isValidPassword(request.getNewPassword())) {
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Invalid new password format");
        }
        var user = this.provider.authenticate(request.getEmail(), request.getOldPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        if (!Objects.equals(user.getStatus(), Status.ACTIVE)) {
            user.setStatus(Status.ACTIVE);
        }
        userRepository.save(user);
    }
}
