package com.tenpearls.contactmanagement.auth.controller;

import com.tenpearls.contactmanagement.auth.dto.AuthResponse;
import com.tenpearls.contactmanagement.auth.dto.LoginRequest;
import com.tenpearls.contactmanagement.auth.dto.RegisterRequest;
import com.tenpearls.contactmanagement.auth.service.AuthService;
import com.tenpearls.contactmanagement.common.exception.DuplicateResourceException;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("POST /api/v1/auth/register received");
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (DataIntegrityViolationException ex) {
            if (!isUniqueConstraintViolation(ex)) {
                throw ex;
            }
            throw new DuplicateResourceException("An account with this email or phone number already exists.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("POST /api/v1/auth/login received");
        return ResponseEntity.ok(authService.login(request));
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                for (SQLException sql = sqlException; sql != null; sql = sql.getNextException()) {
                    if ("23505".equals(sql.getSQLState()) || sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
