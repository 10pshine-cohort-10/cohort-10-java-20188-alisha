package com.tenpearls.contactmanagement.user.controller;

import com.tenpearls.contactmanagement.user.dto.ChangePasswordRequest;
import com.tenpearls.contactmanagement.user.dto.UpdateProfileRequest;
import com.tenpearls.contactmanagement.user.dto.UserResponse;
import com.tenpearls.contactmanagement.user.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName();
        log.debug("GET /api/v1/users/me requested by user [{}]", userId);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        String userId = authentication.getName();
        log.info("PUT /api/v1/users/me requested by user [{}]", userId);
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request) {
        String userId = authentication.getName();
        log.info("POST /api/v1/users/change-password requested by user [{}]", userId);
        userService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
    }
}
