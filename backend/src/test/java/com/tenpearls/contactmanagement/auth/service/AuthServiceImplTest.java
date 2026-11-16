package com.tenpearls.contactmanagement.auth.service;

import com.tenpearls.contactmanagement.auth.dto.AuthResponse;
import com.tenpearls.contactmanagement.auth.dto.RegisterRequest;
import com.tenpearls.contactmanagement.user.entity.User;
import com.tenpearls.contactmanagement.common.exception.DuplicateResourceException;
import com.tenpearls.contactmanagement.user.repository.UserRepository;
import com.tenpearls.contactmanagement.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("Should register user with phone number and issue JWT")
    @SuppressWarnings("null")
    void registerWithPhone_Success() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Priya Patel");
        req.setIdentifier("+14155550199");
        req.setPassword("Password123!");

        when(userRepository.existsByPhone("+14155550199")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenProvider.generateToken(any())).thenReturn("jwt-token-123");

        AuthResponse response = authService.register(req);

        assertEquals("jwt-token-123", response.getToken());
        assertEquals("+14155550199", response.getUser().getPhone());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject duplicate email registration")
    void registerDuplicateEmail_ThrowsConflict() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Alex");
        req.setIdentifier("alex@apex.com");
        req.setPassword("Password123!");

        when(userRepository.existsByEmailIgnoreCase("alex@apex.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(req));
    }

}
