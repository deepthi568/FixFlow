package FixFlow_backend.controller;

import FixFlow_backend.model.AuditEvent;
import FixFlow_backend.repository.AuditEventRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-events")
@PreAuthorize("hasRole('ADMIN')")
public class AuditEventController {
    private final AuditEventRepository events;

    public AuditEventController(AuditEventRepository events) { this.events = events; }

    @GetMapping
    public List<AuditEvent> list(@RequestParam(required = false) String recordType,
                                 @RequestParam(required = false) Long recordId) {
        if (recordType != null && !recordType.isBlank() && recordId != null) {
            return events.findTop200ByRecordTypeAndRecordIdOrderByOccurredAtDesc(recordType.trim().toUpperCase(), recordId);
        }
        return events.findTop200ByOrderByOccurredAtDesc();
    }
}
