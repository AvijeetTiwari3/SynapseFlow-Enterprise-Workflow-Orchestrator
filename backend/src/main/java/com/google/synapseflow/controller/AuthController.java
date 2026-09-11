package com.google.synapseflow.controller;

import com.google.synapseflow.dto.ApiResponse;
import com.google.synapseflow.dto.AuthDto;
import com.google.synapseflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and profile inspection")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and generate JWT token")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new enterprise user")
    public ResponseEntity<ApiResponse<AuthDto.UserProfileDto>> register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        AuthDto.UserProfileDto profile = authService.register(request);
        return ResponseEntity.ok(ApiResponse.ok("User registered successfully", profile));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<AuthDto.UserProfileDto>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        AuthDto.UserProfileDto profile = authService.getCurrentUserProfile(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
