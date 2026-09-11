package com.google.synapseflow.dto;

import com.google.synapseflow.entity.Role;
import jakarta.validation.constraints.NotBlank;
import java.util.Set;

public class AuthDto {

    public record LoginRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password
    ) {}

    public record RegisterRequest(
        @NotBlank(message = "Username is required") String username,
        @NotBlank(message = "Password is required") String password,
        @NotBlank(message = "Email is required") String email,
        @NotBlank(message = "Full name is required") String fullName,
        String department,
        Set<Role> roles
    ) {}

    public record AuthResponse(
        String token,
        String tokenType,
        String username,
        String email,
        String fullName,
        String department,
        Set<String> roles,
        long expiresIn
    ) {}

    public record UserProfileDto(
        Long id,
        String username,
        String email,
        String fullName,
        String department,
        Set<Role> roles,
        boolean enabled
    ) {}
}
