package com.tenpearls.contactmanagement.user.service;

import com.tenpearls.contactmanagement.common.exception.InvalidCredentialsException;
import com.tenpearls.contactmanagement.common.exception.ResourceNotFoundException;
import com.tenpearls.contactmanagement.user.dto.ChangePasswordRequest;
import com.tenpearls.contactmanagement.user.dto.UpdateProfileRequest;
import com.tenpearls.contactmanagement.user.dto.UserResponse;
import com.tenpearls.contactmanagement.user.entity.User;
import com.tenpearls.contactmanagement.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(String userId) {
        User user = findUser(userId, "User profile not found for ID: ");
        return UserResponse.fromEntity(user);
    }

    @Override
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUser(userId, "User profile not found for ID: ");
        user.setFullName(request.getFullName().trim());
        String phone = request.getPhone() != null && !request.getPhone().isBlank()
                ? request.getPhone().trim()
                : null;
        if (phone == null && user.getEmail() == null) {
            throw new IllegalArgumentException("An account must keep at least one login identifier (email or phone).");
        }
        user.setPhone(phone);
        user.setCompany(request.getCompany() != null && !request.getCompany().isBlank() ? request.getCompany().trim() : null);

        User updated = userRepository.save(user);
        log.info("Updated profile for user ID [{}]", userId);
        return UserResponse.fromEntity(updated);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        log.info("Password change requested for user ID [{}]", userId);
        User user = findUser(userId, "User not found for ID: ");

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change rejected: current password mismatch for user [{}]", userId);
            throw new InvalidCredentialsException("Current password does not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("Password updated successfully for user ID [{}]", userId);
    }

    private User findUser(String userId, String messagePrefix) {
        return userRepository.findById(Objects.requireNonNull(userId, "userId"))
                .orElseThrow(() -> new ResourceNotFoundException(messagePrefix + userId));
    }
}
