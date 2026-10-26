package FixFlow_backend.service;

import FixFlow_backend.model.AuditEvent;
import FixFlow_backend.repository.AuditEventRepository;
import FixFlow_backend.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuditService {
    private final AuditEventRepository events;

    public AuditService(AuditEventRepository events) { this.events = events; }

    public void record(String actionType, String recordType, Long recordId, String details) {
        String actor = SecurityUtils.currentUser().map(user -> user.getUsername()).orElse("system");
        String safeDetails = details == null ? null : details.substring(0, Math.min(details.length(), 2000));
        events.save(new AuditEvent(actor, actionType, recordType, recordId, safeDetails));
    }
}
