package com.tenpearls.contactmanagement.auth.service;

import com.tenpearls.contactmanagement.auth.dto.AuthResponse;
import com.tenpearls.contactmanagement.auth.dto.LoginRequest;
import com.tenpearls.contactmanagement.auth.dto.RegisterRequest;
import com.tenpearls.contactmanagement.user.entity.User;
import com.tenpearls.contactmanagement.common.exception.DuplicateResourceException;
import com.tenpearls.contactmanagement.common.exception.InvalidCredentialsException;
import com.tenpearls.contactmanagement.user.repository.UserRepository;
import com.tenpearls.contactmanagement.security.JwtTokenProvider;
import com.tenpearls.contactmanagement.user.dto.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        String identifier = request.getIdentifier().trim();
        boolean isEmail = identifier.contains("@");
        log.info("Processing user registration using {} identifier", isEmail ? "email" : "phone");

        if (isEmail && userRepository.existsByEmailIgnoreCase(identifier)) {
            log.warn("Duplicate email registration attempt");
            throw new DuplicateResourceException("An account with this email address already exists.");
        }
        if (!isEmail && userRepository.existsByPhone(identifier)) {
            log.warn("Duplicate phone registration attempt");
            throw new DuplicateResourceException("An account with this phone number already exists.");
        }

        User user = new User();
        user.setId("usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        user.setFullName(request.getFullName().trim());
        if (isEmail) {
            user.setEmail(identifier.toLowerCase());
        } else {
            user.setPhone(identifier);
        }
        user.setCompany(request.getCompany() != null && !request.getCompany().isBlank()
                ? request.getCompany().trim()
                : "Enterprise Organization");
        user.setRole("Standard User");
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        String token = tokenProvider.generateToken(saved.getId());
        log.info("Successfully registered new user ID [{}]", saved.getId());

        return new AuthResponse(token, "Account registered successfully.", UserResponse.fromEntity(saved));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        log.info("Authenticating user");

        Optional<User> userOpt = identifier.contains("@")
                ? userRepository.findByEmailIgnoreCase(identifier)
                : userRepository.findByPhone(identifier);

        User user = userOpt.orElseThrow(() -> {
            log.warn("Login failed: no account found");
            return new InvalidCredentialsException("Invalid email/phone or password.");
        });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: incorrect password for user [{}]", user.getId());
            throw new InvalidCredentialsException("Invalid email/phone or password.");
        }

        user.setLastLogin(OffsetDateTime.now());
        userRepository.save(user);

        String token = tokenProvider.generateToken(user.getId());
        log.info("User [{}] authenticated successfully.", user.getId());

        return new AuthResponse(token, "Welcome back, " + user.getFullName() + "!", UserResponse.fromEntity(user));
    }

}
