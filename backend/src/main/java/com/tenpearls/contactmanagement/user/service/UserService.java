package com.tenpearls.contactmanagement.user.service;

import com.tenpearls.contactmanagement.user.dto.ChangePasswordRequest;
import com.tenpearls.contactmanagement.user.dto.UpdateProfileRequest;
import com.tenpearls.contactmanagement.user.dto.UserResponse;

public interface UserService {
    UserResponse getProfile(String userId);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    void changePassword(String userId, ChangePasswordRequest request);
}
