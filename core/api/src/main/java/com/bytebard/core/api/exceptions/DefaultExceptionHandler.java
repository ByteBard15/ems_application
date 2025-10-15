package com.bytebard.core.api.exceptions;

import com.bytebard.core.api.types.MvcApiReponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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
    public ResponseEntity<MvcApiReponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.BAD_REQUEST, false, String.join(", ", errors));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({NotFoundException.class, UsernameNotFoundException.class, EntityNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<MvcApiReponse<Object>> handleNotFoundExceptions(Exception ex) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.NOT_FOUND, false, ex.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler({HttpClientErrorException.class})
    public ResponseEntity<MvcApiReponse<Object>> handleClientException(Exception ex) {
        if (ex instanceof HttpClientErrorException clientEx) {
            var status = HttpStatus.valueOf(clientEx.getStatusCode().value());
            MvcApiReponse<Object> response = new MvcApiReponse<>(status, false, clientEx.getStatusText());
            return ResponseEntity.status(status).body(response);
        }
        HttpStatus status = HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(new MvcApiReponse<>(status, false, ex.getMessage()));
    }

    @ExceptionHandler(value = {Exception.class})
    public ResponseEntity<MvcApiReponse<Object>> handleGenericException(Exception ex) {
        log.error(ex.getMessage(), ex);
        var response = new MvcApiReponse<Object>(HttpStatus.INTERNAL_SERVER_ERROR, false, "An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler({SecurityException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<MvcApiReponse<Object>> handleForbidden(Exception ex) {
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.FORBIDDEN, false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MvcApiReponse<Object>> handleUnauthorized(BadCredentialsException ex) {
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.UNAUTHORIZED, false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<MvcApiReponse<Object>> handleConflict(DataIntegrityViolationException ex) {
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.CONFLICT, false, "Conflict: " + ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<MvcApiReponse<Object>> handleUnauthorized(Exception ex) {
        MvcApiReponse<Object> response = new MvcApiReponse<>(HttpStatus.UNAUTHORIZED, false, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
}

