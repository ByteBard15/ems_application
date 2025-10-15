package com.bytebard.core.api.validators;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

public class FieldValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-]).{8,64}$"
    );

    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        if (!StringUtils.hasText(password)) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }
}
