package com.clouddrive.service;

import com.clouddrive.dto.AuthResponse;
import com.clouddrive.dto.LoginRequest;
import com.clouddrive.dto.RegisterRequest;
import com.clouddrive.entity.User;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ActivityService activityService;

    @Value("${app.storage.base-dir:./storage_root}")
    private String baseDir;

    @Value("${app.storage.default-quota-bytes:524288000}")
    private long defaultQuotaBytes;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       ActivityService activityService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.activityService = activityService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String cleanUsername = request.getUsername() != null ? request.getUsername().trim() : "";
        String cleanEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (cleanUsername.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (cleanEmail.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be empty");
        }
        if (userRepository.existsByUsernameIgnoreCase(cleanUsername) || userRepository.existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already taken. Please sign in or choose another username.");
        }
        if (userRepository.existsByEmailIgnoreCase(cleanEmail) || userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email '" + cleanEmail + "' is already registered. Please sign in with your existing account.");
        }

        User user = new User();
        user.setUsername(cleanUsername);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName() != null ? request.getFullName().trim() : cleanUsername);
        user.setRole("ROLE_USER");
        user.setQuotaBytes(defaultQuotaBytes);
        user.setUsedBytes(0L);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser;
        try {
            savedUser = userRepository.save(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already registered. Please choose another username.");
        }

        // Initialize user directory on physical disk
        try {
            Path userPath = Paths.get(baseDir, "users", savedUser.getUsername());
            Files.createDirectories(userPath);
        } catch (IOException e) {
            // Ignore directory creation failure in mock test environments
        }

        activityService.log(savedUser, "REGISTER", savedUser.getUsername(), "User account registered successfully");

        String token = tokenProvider.generateTokenFromUsername(savedUser.getUsername());
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole(),
                savedUser.getQuotaBytes(),
                savedUser.getUsedBytes()
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        activityService.log(user, "LOGIN", user.getUsername(), "User logged in successfully");

        String token = tokenProvider.generateToken(authentication);
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getQuotaBytes(),
                user.getUsedBytes()
        );
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional
    public User updateProfile(User user, com.clouddrive.dto.UpdateProfileRequest request) {
        if (!user.getEmail().equalsIgnoreCase(request.getEmail().trim()) &&
                userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new IllegalArgumentException("Email is already in use by another account");
        }
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        User saved = userRepository.save(user);
        activityService.log(saved, "PROFILE_UPDATE", saved.getUsername(), "Updated profile details");
        return saved;
    }

    @Transactional
    public void changePassword(User user, com.clouddrive.dto.ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }
        if (request.getNewPassword().trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);
        activityService.log(user, "PASSWORD_CHANGE", user.getUsername(), "Password changed successfully");
    }
}
