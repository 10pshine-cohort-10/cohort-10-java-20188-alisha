package com.tenpearls.contactmanagement.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "Email or phone number is required")
    @Size(max = 255, message = "Email or phone number must be at most 255 characters")
    private String identifier;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(max = 150, message = "Company must be at most 150 characters")
    private String company;

    @jakarta.validation.constraints.AssertTrue(message = "Phone number must be at most 50 characters")
    public boolean isIdentifierWithinStorageLimit() {
        return identifier == null || identifier.contains("@") || identifier.length() <= 50;
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdentifier() { return identifier; }
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
}
