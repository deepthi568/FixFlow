package FixFlow_backend.controller;

import FixFlow_backend.dto.AuthResponse;
import FixFlow_backend.dto.LoginRequest;
import FixFlow_backend.dto.UserResponse;
import FixFlow_backend.security.AuthUserDetails;
import FixFlow_backend.security.SecurityUtils;
import FixFlow_backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Map<String, String> logout(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        String jti = (String) request.getAttribute("jwt.jti");
        Date exp = (Date) request.getAttribute("jwt.exp");
        authService.logout(token, jti, exp);
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse me() {
        AuthUserDetails user = SecurityUtils.requireUser();
        return UserResponse.from(user);
    }
}
