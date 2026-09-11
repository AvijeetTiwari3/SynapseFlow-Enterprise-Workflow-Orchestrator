package com.google.synapseflow.service;

import com.google.synapseflow.dto.AuthDto;
import com.google.synapseflow.entity.Role;
import com.google.synapseflow.entity.User;
import com.google.synapseflow.exception.AppException;
import com.google.synapseflow.repository.UserRepository;
import com.google.synapseflow.security.JwtTokenProvider;
import com.google.synapseflow.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new AppException("User not found: " + principal.getUsername()));

        Set<String> roles = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AuthDto.AuthResponse(
                jwt,
                "Bearer",
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                roles,
                tokenProvider.getExpirationMs()
        );
    }

    @Transactional
    public AuthDto.UserProfileDto register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new AppException("Username is already taken!");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new AppException("Email address is already in use!");
        }

        Set<Role> roles = request.roles();
        if (roles == null || roles.isEmpty()) {
            roles = new HashSet<>();
            roles.add(Role.ROLE_APPROVER);
        }

        User user = new User(
                request.username(),
                passwordEncoder.encode(request.password()),
                request.email(),
                request.fullName(),
                request.department(),
                roles
        );

        User saved = userRepository.save(user);

        return new AuthDto.UserProfileDto(
                saved.getId(),
                saved.getUsername(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getDepartment(),
                saved.getRoles(),
                saved.isEnabled()
        );
    }

    @Transactional(readOnly = true)
    public AuthDto.UserProfileDto getCurrentUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException("User not found: " + username));

        return new AuthDto.UserProfileDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getDepartment(),
                user.getRoles(),
                user.isEnabled()
        );
    }
}
