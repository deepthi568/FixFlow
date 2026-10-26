package FixFlow_backend.repository;

import FixFlow_backend.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findTop200ByOrderByOccurredAtDesc();
    List<AuditEvent> findTop200ByRecordTypeAndRecordIdOrderByOccurredAtDesc(String recordType, Long recordId);
}
