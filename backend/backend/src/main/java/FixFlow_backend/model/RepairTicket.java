package FixFlow_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_tickets")
public class RepairTicket {

    // ── Valid sets (shared with service layer) ─────────────────────────────

    public static final java.util.Set<String> VALID_STATUSES = java.util.Set.of(
            "RECEIVED", "DIAGNOSED", "IN_REPAIR", "QUALITY_CHECK",
            "READY", "DELIVERED", "ON_HOLD", "CANCELLED"
    );

    public static final java.util.Set<String> VALID_PRIORITIES = java.util.Set.of(
            "HIGH", "MEDIUM", "LOW"
    );

    // ── Primary key ────────────────────────────────────────────────────────

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Customer ──────────────────────────────────────────────────────────

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Customer phone is required")
    private String customerPhone;

    /**
     * Optional – validated only when a value is present (null is allowed).
     * A blank string sent from the client is normalised to null in the setter.
     */
    @Email(message = "Customer email must be a valid email address")
    private String customerEmail;

    // ── Device / Problem ──────────────────────────────────────────────────

    @NotBlank(message = "Device name is required")
    private String deviceName;

    @NotBlank(message = "Problem description is required")
    @Column(columnDefinition = "TEXT")
    private String problemDescription;

    // ── Workflow ──────────────────────────────────────────────────────────

    /**
     * Ticket lifecycle status.
     * Validated by the service layer against {@link #VALID_STATUSES}; stored as a plain VARCHAR.
     */
    private String status = "RECEIVED";

    /**
     * Ticket priority: HIGH, MEDIUM (default), or LOW.
     * Validated by the service layer against {@link #VALID_PRIORITIES}.
     */
    private String priority = "MEDIUM";

    /**
     * Optional target completion date supplied by the service desk on intake.
     * Stored as DATE in MySQL (no time component needed).
     */
    private LocalDate targetCompletionDate;

    /**
     * Optional estimated repair cost.
     * Must be ≥ 0.00; stored as DECIMAL(12,2) in MySQL.
     */
    @DecimalMin(value = "0.00", message = "Estimated cost must not be negative")
    @Digits(integer = 10, fraction = 2,
            message = "Estimated cost must be a valid monetary amount (max 10 integer + 2 decimal digits)")
    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedCost;

    /** Assigned technician – nullable so existing tickets remain valid. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "technician_id")
    private Technician technician;

    /**
     * Soft-delete flag.  columnDefinition ensures MySQL sets existing rows to 0
     * when the column is added via ddl-auto=update.
     */
    @Column(nullable = false)
    private boolean deleted = false;

    // ── Timestamps ────────────────────────────────────────────────────────

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Constructor ───────────────────────────────────────────────────────

    public RepairTicket() {}

    // ── Getters ───────────────────────────────────────────────────────────

    public Long getId()                          { return id; }
    public String getCustomerName()              { return customerName; }
    public String getCustomerPhone()             { return customerPhone; }
    public String getCustomerEmail()             { return customerEmail; }
    public String getDeviceName()                { return deviceName; }
    public String getProblemDescription()        { return problemDescription; }
    public String getStatus()                    { return status; }
    public String getPriority()                  { return priority; }
    public LocalDate getTargetCompletionDate()   { return targetCompletionDate; }
    public BigDecimal getEstimatedCost()         { return estimatedCost; }
    public Technician getTechnician()            { return technician; }
    public boolean isDeleted()                   { return deleted; }
    public LocalDateTime getCreatedAt()          { return createdAt; }
    public LocalDateTime getUpdatedAt()          { return updatedAt; }

    // ── Setters ───────────────────────────────────────────────────────────

    public void setCustomerName(String customerName)       { this.customerName = customerName; }
    public void setCustomerPhone(String customerPhone)     { this.customerPhone = customerPhone; }

    /** Normalises blank strings to null so @Email validation is skipped for missing emails. */
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = (customerEmail != null && customerEmail.isBlank()) ? null : customerEmail;
    }

    public void setDeviceName(String deviceName)           { this.deviceName = deviceName; }
    public void setProblemDescription(String desc)         { this.problemDescription = desc; }
    public void setStatus(String status)                   { this.status = status; }
    public void setPriority(String priority)               { this.priority = priority; }
    public void setTargetCompletionDate(LocalDate d)       { this.targetCompletionDate = d; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public void setTechnician(Technician technician)       { this.technician = technician; }
    public void setDeleted(boolean deleted)                { this.deleted = deleted; }
}
