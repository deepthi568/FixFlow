package FixFlow_backend.repository;

import FixFlow_backend.model.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {
    long deleteByExpiresAtBefore(Instant now);
}
