package com.bytebard.core.api.exceptions;

import com.bytebard.core.api.types.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class DefaultExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionHandler.class);

    @ExceptionHandler({MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.BAD_REQUEST, false, String.join(", ", errors));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({NotFoundException.class, UsernameNotFoundException.class, EntityNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFoundExceptions(Exception ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.NOT_FOUND, false, ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler({HttpClientErrorException.class})
    public ResponseEntity<ApiResponse<Object>> handleClientException(Exception ex) {
        if (ex instanceof HttpClientErrorException clientEx) {
            var status = HttpStatus.valueOf(clientEx.getStatusCode().value());
            ApiResponse<Object> response = new ApiResponse<>(status, false, clientEx.getStatusText());
            return ResponseEntity.status(status).body(response);
        }
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new ApiResponse<>(status, false, ex.getMessage()));
    }

    @ExceptionHandler(value = {Exception.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error(ex.getMessage(), ex);
        var response = new ApiResponse<Object>(HttpStatus.INTERNAL_SERVER_ERROR, false, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(SecurityException ex) {
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.FORBIDDEN, false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConflict(DataIntegrityViolationException ex) {
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.CONFLICT, false, "Conflict: " + ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleUnauthorized(Exception ex) {
        ApiResponse<Object> response = new ApiResponse<>(HttpStatus.UNAUTHORIZED, false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}

