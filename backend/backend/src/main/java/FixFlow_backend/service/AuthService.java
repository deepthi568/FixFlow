package FixFlow_backend.service;

import FixFlow_backend.dto.AuthResponse;
import FixFlow_backend.dto.LoginRequest;
import FixFlow_backend.dto.UserResponse;
import FixFlow_backend.security.AuthUserDetails;
import FixFlow_backend.security.JwtService;
import FixFlow_backend.security.TokenBlacklist;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Date;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklist tokenBlacklist;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       TokenBlacklist tokenBlacklist) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.tokenBlacklist = tokenBlacklist;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            AuthUserDetails user = (AuthUserDetails) authentication.getPrincipal();
            String token = jwtService.createToken(user);
            Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());
            return new AuthResponse(token, expiresAt, UserResponse.from(user));
        } catch (DisabledException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account is disabled");
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public void logout(String rawToken, String jti, Date expiration) {
        if (jti != null) {
            Instant exp = expiration != null ? expiration.toInstant() : Instant.now().plusMillis(jwtService.getExpirationMs());
            tokenBlacklist.revoke(jti, exp);
            return;
        }
        if (rawToken != null && !rawToken.isBlank()) {
            try {
                var claims = jwtService.parse(rawToken);
                tokenBlacklist.revoke(claims.getId(),
                        claims.getExpiration() != null ? claims.getExpiration().toInstant() : Instant.now());
            } catch (ResponseStatusException ignored) {
                // Already invalid; nothing to revoke.
            }
        }
    }
}
