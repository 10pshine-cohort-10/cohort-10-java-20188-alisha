package com.tenpearls.contactmanagement.user.service;

import com.tenpearls.contactmanagement.common.exception.InvalidCredentialsException;
import com.tenpearls.contactmanagement.user.dto.ChangePasswordRequest;
import com.tenpearls.contactmanagement.user.entity.User;
import com.tenpearls.contactmanagement.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId("usr_001");
        existingUser.setPasswordHash("$2a$10$hashedPassword");
    }

    @Test
    @DisplayName("Should change password when current password matches")
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        when(userRepository.findById("usr_001")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("OldPass123!", "$2a$10$hashedPassword")).thenReturn(true);
        when(passwordEncoder.encode("NewPass123!")).thenReturn("newEncodedHash");

        userService.changePassword("usr_001", request);

        assertEquals("newEncodedHash", existingUser.getPasswordHash());
        verify(userRepository).save(Objects.requireNonNull(existingUser));
    }

    @Test
    @DisplayName("Should reject password change when current password is incorrect")
    @SuppressWarnings("null")
    void changePassword_WrongCurrent_Throws() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPass!");
        request.setNewPassword("NewPass123!");

        when(userRepository.findById("usr_001")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPass!", "$2a$10$hashedPassword")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> userService.changePassword("usr_001", request));
        verify(userRepository, never()).save(any(User.class));
    }
}
