package FixFlow_backend.dto;

import java.time.Instant;

public class AuthResponse {

    private final String token;
    private final String tokenType = "Bearer";
    private final Instant expiresAt;
    private final UserResponse user;

    public AuthResponse(String token, Instant expiresAt, UserResponse user) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public Instant getExpiresAt() { return expiresAt; }
    public UserResponse getUser() { return user; }
}
