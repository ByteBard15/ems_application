package com.bytebard.auth.controllers;

import com.bytebard.auth.service.AuthService;
import com.bytebard.auth.types.ChangePasswordRequest;
import com.bytebard.auth.types.LoginRequest;
import com.bytebard.auth.types.LoginResponse;
import com.bytebard.core.api.constants.Routes;
import com.bytebard.core.api.types.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(Routes.AUTH)
@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(Routes.LOGIN)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        var result = authService.login(loginRequest);
        var loginResponse = new ApiResponse<>(result, HttpStatus.OK, true);
        return ResponseEntity.ok().body(loginResponse);
    }

    @PostMapping(Routes.CHANGE_PASSWORD)
    public ResponseEntity<ApiResponse<Object>> changePassword(@RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        var loginResponse = new ApiResponse<>(null, HttpStatus.OK, true);
        return ResponseEntity.ok().body(loginResponse);
    }
}
