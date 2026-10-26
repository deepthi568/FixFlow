package FixFlow_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 64)
    private String sku;

    @Column(length = 500)
    private String description;

    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private int quantity = 0;

    /** Null is treated as zero for rows created before the reorder threshold was added. */
    @Min(value = 0, message = "Reorder threshold cannot be negative")
    private Integer reorderThreshold = 0;

    @Column(length = 32)
    private String unit;

    @Column(length = 255)
    private String notes;

    @Column(nullable = true)
    private Boolean active = true;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 64)
    private String createdBy;

    @Column(length = 64)
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public String getDescription() { return description; }
    public int getQuantity() { return quantity; }
    public int getReorderThreshold() { return reorderThreshold == null ? 0 : reorderThreshold; }
    public String getUnit() { return unit; }
    public String getNotes() { return notes; }
    public boolean isActive() { return active == null || active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    public void setName(String name) { this.name = name; }
    public void setSku(String sku) { this.sku = sku; }
    public void setDescription(String description) { this.description = description; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public void setReorderThreshold(Integer reorderThreshold) { this.reorderThreshold = reorderThreshold; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setActive(boolean active) { this.active = active; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
