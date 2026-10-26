package FixFlow_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Persisted revocation record for a logged-out access token. */
@Entity
@Table(name = "revoked_tokens")
public class RevokedToken {

    @Id
    @Column(length = 64, nullable = false)
    private String tokenId;

    @Column(nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {}

    public RevokedToken(String tokenId, Instant expiresAt) {
        this.tokenId = tokenId;
        this.expiresAt = expiresAt;
    }

    public Instant getExpiresAt() { return expiresAt; }
}
