package FixFlow_backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Append-only record of a business action; the application exposes no write or delete API. */
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_record", columnList = "record_type,record_id")
})
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String actor;

    @Column(nullable = false, length = 64)
    private String actionType;

    @Column(name = "record_type", nullable = false, length = 48)
    private String recordType;

    @Column(name = "record_id", nullable = false)
    private Long recordId;

    @Column(length = 2000)
    private String details;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    protected AuditEvent() {}

    public AuditEvent(String actor, String actionType, String recordType, Long recordId, String details) {
        this.actor = actor;
        this.actionType = actionType;
        this.recordType = recordType;
        this.recordId = recordId;
        this.details = details;
    }

    @PrePersist
    protected void onCreate() { occurredAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getActor() { return actor; }
    public String getActionType() { return actionType; }
    public String getRecordType() { return recordType; }
    public Long getRecordId() { return recordId; }
    public String getDetails() { return details; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
