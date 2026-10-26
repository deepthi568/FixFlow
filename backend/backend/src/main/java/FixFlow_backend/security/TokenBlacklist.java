package FixFlow_backend.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import FixFlow_backend.model.RevokedToken;
import FixFlow_backend.repository.RevokedTokenRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Database-backed denylist of JWT ids ({@code jti}) that have been logged out.
 * Persisting revocations ensures logout remains effective across restarts and instances.
 */
@Component
public class TokenBlacklist {

    private final RevokedTokenRepository revokedTokens;

    public TokenBlacklist(RevokedTokenRepository revokedTokens) {
        this.revokedTokens = revokedTokens;
    }

    @Transactional
    public void revoke(String jti, Instant expiresAt) {
        if (jti == null || jti.isBlank()) {
            return;
        }
        revokedTokens.save(new RevokedToken(jti, expiresAt != null ? expiresAt : Instant.now()));
    }

    @Transactional
    public boolean isRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return revokedTokens.findById(jti)
                .map(token -> {
                    if (token.getExpiresAt().isBefore(Instant.now())) {
                        revokedTokens.delete(token);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    @Scheduled(fixedDelay = 3_600_000)
    public void purgeExpired() {
        revokedTokens.deleteByExpiresAtBefore(Instant.now());
    }
}
