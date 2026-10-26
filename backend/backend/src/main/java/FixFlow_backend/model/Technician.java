package FixFlow_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "technicians")
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Technician name is required")
    @Column(nullable = false)
    private String name;

    @Email(message = "Email must be a valid email address")
    private String email;

    private String phone;

    private String specialty;

    @Size(max = 1000, message = "Skills must not exceed 1000 characters")
    @Column(length = 1000)
    private String skills;

    /** AVAILABLE, BUSY, or UNAVAILABLE. Nullable for compatibility with existing rows. */
    @Column(length = 24)
    private String availability = "AVAILABLE";

    @Column(nullable = false)
    private boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Technician() {}

    // --- Getters ---

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getPhone() { return phone; }

    public String getSpecialty() { return specialty; }

    public String getSkills() { return skills; }

    public String getAvailability() { return availability == null ? "AVAILABLE" : availability; }

    public boolean isActive() { return active; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---

    public void setName(String name) { this.name = name; }

    public void setEmail(String email) {
        this.email = (email != null && email.isBlank()) ? null : email;
    }

    public void setPhone(String phone) { this.phone = phone; }

    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public void setSkills(String skills) { this.skills = skills; }

    public void setAvailability(String availability) { this.availability = availability; }

    public void setActive(boolean active) { this.active = active; }
}
