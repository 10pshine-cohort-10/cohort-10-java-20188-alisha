package com.tenpearls.contactmanagement.auth.service;

import com.tenpearls.contactmanagement.auth.dto.AuthResponse;
import com.tenpearls.contactmanagement.auth.dto.LoginRequest;
import com.tenpearls.contactmanagement.auth.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
