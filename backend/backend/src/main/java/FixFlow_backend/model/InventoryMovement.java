package FixFlow_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Audit record for every stock creation, adjustment, restock, or ticket consumption. */
@Entity
@Table(name = "inventory_movements")
public class InventoryMovement {
    public enum Type { CREATED, ADJUSTMENT, RESTOCK, CONSUMPTION }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_item_id", nullable = false)
    @JsonIgnore
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repair_ticket_id")
    @JsonIgnore
    private RepairTicket ticket;

    @Column(nullable = false)
    private int quantityChange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Type type;

    @Column(nullable = false, length = 64)
    private String performedBy;

    @Column(length = 255)
    private String note;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected InventoryMovement() {}

    public InventoryMovement(InventoryItem item, RepairTicket ticket, int quantityChange,
                             Type type, String performedBy, String note) {
        this.item = item;
        this.ticket = ticket;
        this.quantityChange = quantityChange;
        this.type = type;
        this.performedBy = performedBy;
        this.note = note;
    }

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public InventoryItem getItem() { return item; }
    public RepairTicket getTicket() { return ticket; }
    public int getQuantityChange() { return quantityChange; }
    public Type getType() { return type; }
    public String getPerformedBy() { return performedBy; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
