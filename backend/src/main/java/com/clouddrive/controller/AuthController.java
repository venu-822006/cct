package com.clouddrive.controller;

import com.clouddrive.dto.*;
import com.clouddrive.entity.User;
import com.clouddrive.security.RateLimitingService;
import com.clouddrive.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitingService rateLimitingService;
    private final com.clouddrive.service.RecaptchaService recaptchaService;
    private final com.clouddrive.service.TurnstileService turnstileService;

    public AuthController(AuthService authService,
                          RateLimitingService rateLimitingService,
                          com.clouddrive.service.RecaptchaService recaptchaService,
                          com.clouddrive.service.TurnstileService turnstileService) {
        this.authService = authService;
        this.rateLimitingService = rateLimitingService;
        this.recaptchaService = recaptchaService;
        this.turnstileService = turnstileService;
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitingService.allowRegister(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(Map.of("error", "Too many registration attempts. Please wait 1 minute before trying again."));
        }

        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Registration failed"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        if (!rateLimitingService.allowLogin(clientIp)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "60")
                    .body(Map.of("error", "Too many login attempts. Please wait 1 minute before trying again."));
        }

        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }
    }

    @PostMapping("/verify-recaptcha")
    public ResponseEntity<?> verifyRecaptcha(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("recaptchaToken") : null;
        if (recaptchaService.verifyToken(token)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Google reCAPTCHA verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Google reCAPTCHA verification failed. Please try again."));
        }
    }

    @PostMapping("/verify-turnstile")
    public ResponseEntity<?> verifyTurnstile(@RequestBody Map<String, String> body) {
        String token = body != null ? body.get("turnstileToken") : null;
        if (turnstileService.verifyToken(token)) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Cloudflare Turnstile verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Cloudflare Turnstile verification failed. Please try again."));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        try {
            User user = authService.getUserByUsername(userDetails.getUsername());
            return ResponseEntity.ok(new AuthResponse(
                    null,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getQuotaBytes(),
                    user.getUsedBytes()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Session expired or user not found"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                           @Valid @RequestBody UpdateProfileRequest request) {
        try {
            User user = authService.getUserByUsername(userDetails.getUsername());
            User updated = authService.updateProfile(user, request);
            return ResponseEntity.ok(new AuthResponse(
                    null,
                    updated.getId(),
                    updated.getUsername(),
                    updated.getEmail(),
                    updated.getFullName(),
                    updated.getRole(),
                    updated.getQuotaBytes(),
                    updated.getUsedBytes()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal UserDetails userDetails,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            User user = authService.getUserByUsername(userDetails.getUsername());
            authService.changePassword(user, request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
