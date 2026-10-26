package FixFlow_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Application user account stored in the {@code app_users} table.
 *
 * <p>Three roles are supported:
 * <ul>
 *   <li>{@code ADMIN}         – full access to all resources</li>
 *   <li>{@code SERVICE_STAFF} – create / view / update tickets and customers</li>
 *   <li>{@code TECHNICIAN}    – view and update only their own assigned tickets</li>
 * </ul>
 * </p>
 *
 * <p>Passwords are stored as BCrypt hashes; plain-text passwords are never persisted.</p>
 */
@Entity
@Table(name = "app_users",
       uniqueConstraints = @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"))
public class AppUser {

    public enum Role { ADMIN, SERVICE_STAFF, TECHNICIAN }

    // ── Primary key ────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Credentials ────────────────────────────────────────────────────────

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64, message = "Username must be between 3 and 64 characters")
    @Column(nullable = false, length = 64)
    private String username;

    /**
     * BCrypt hash of the user's password – never the plain text.
     * The column is named {@code password_hash} to make the intent obvious in SQL.
     */
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // ── Role ──────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Role role;

    // ── Display info ──────────────────────────────────────────────────────

    /** Human-readable full name shown in the UI. */
    @Column(length = 128)
    private String fullName;

    /**
     * Optional link to a {@link Technician} record.
     * Required when role = TECHNICIAN so we know which tickets to show.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    /** When false the account exists but cannot log in. */
    @Column(nullable = false)
    private boolean enabled = true;

    // ── Timestamps ────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── No-arg constructor ────────────────────────────────────────────────

    public AppUser() {}

    // ── Getters ──────────────────────────────────────────────────────────

    public Long getId()               { return id; }
    public String getUsername()       { return username; }
    public String getPasswordHash()   { return passwordHash; }
    public Role getRole()             { return role; }
    public String getFullName()       { return fullName; }
    public Technician getTechnician() { return technician; }
    public boolean isEnabled()        { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────

    public void setUsername(String username)         { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setRole(Role role)                   { this.role = role; }
    public void setFullName(String fullName)         { this.fullName = fullName; }
    public void setTechnician(Technician technician) { this.technician = technician; }
    public void setEnabled(boolean enabled)          { this.enabled = enabled; }
}
