package FixFlow_backend.service;

import FixFlow_backend.dto.TicketStatsDto;
import FixFlow_backend.model.AppUser;
import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.model.Technician;
import FixFlow_backend.repository.RepairTicketRepository;
import FixFlow_backend.repository.TechnicianRepository;
import FixFlow_backend.security.AuthUserDetails;
import FixFlow_backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Business-logic layer for repair tickets.
 *
 * <p>All database writes go through this service so that controllers stay thin
 * and validation / business rules are testable in isolation.</p>
 */
@Service
@Transactional
public class RepairTicketService {

    /** Ordered list used for the byStatus map and chart. */
    private static final List<String> STATUS_ORDER = List.of(
            "RECEIVED", "DIAGNOSED", "IN_REPAIR", "QUALITY_CHECK",
            "READY", "DELIVERED", "ON_HOLD", "CANCELLED"
    );

    private final RepairTicketRepository ticketRepo;
    private final TechnicianRepository   technicianRepo;
    private final AuditService audit;

    public RepairTicketService(RepairTicketRepository ticketRepo,
                               TechnicianRepository technicianRepo,
                               AuditService audit) {
        this.ticketRepo     = ticketRepo;
        this.technicianRepo = technicianRepo;
        this.audit = audit;
    }

    // ── Create ────────────────────────────────────────────────────────────

    /**
     * Create a new ticket.
     *
     * @param ticket  bean-validated by the controller before this method is called
     * @return the persisted ticket
     */
    public RepairTicket createTicket(RepairTicket ticket) {
        validateStatus(ticket.getStatus());
        validatePriority(ticket.getPriority());
        ticket.setDeleted(false);
        RepairTicket saved = ticketRepo.save(ticket);
        audit.record("TICKET_CREATED", "TICKET", saved.getId(),
                "status=" + saved.getStatus() + ";priority=" + saved.getPriority()
                        + ";targetCompletionDate=" + saved.getTargetCompletionDate());
        return saved;
    }

    // ── Read ──────────────────────────────────────────────────────────────

    /**
     * List all non-deleted tickets with optional search and filter support.
     *
     * @param search   free-text substring matched against customer name, device name, or ticket ID
     * @param status   exact status filter (case-insensitive)
     * @param priority exact priority filter (case-insensitive)
     */
    @Transactional(readOnly = true)
    public List<RepairTicket> listTickets(String search, String status, String priority) {
        boolean hasSearch   = search   != null && !search.isBlank();
        boolean hasStatus   = status   != null && !status.isBlank();
        boolean hasPriority = priority != null && !priority.isBlank();

        String searchTrim   = hasSearch   ? search.trim()              : null;
        String statusUpper  = hasStatus   ? status.trim().toUpperCase() : null;
        String priorityUpper = hasPriority ? priority.trim().toUpperCase() : null;

        // If search looks like a number, also check the ID.
        // Must be assigned as an effectively-final value for use inside the lambda.
        final Long idSearch;
        if (hasSearch) {
            Long parsed = null;
            try { parsed = Long.parseLong(searchTrim); } catch (NumberFormatException ignored) {}
            idSearch = parsed;
        } else {
            idSearch = null;
        }

        List<RepairTicket> all = ticketRepo.findByDeletedFalseOrderByCreatedAtDesc();

        List<RepairTicket> filtered = all.stream().filter(t -> {
            // status filter
            if (hasStatus && !t.getStatus().equals(statusUpper)) return false;
            // priority filter
            if (hasPriority && !t.getPriority().equals(priorityUpper)) return false;
            // search filter (ID OR customer name OR device name)
            if (hasSearch) {
                boolean matchId   = idSearch != null && t.getId().equals(idSearch);
                boolean matchCust = t.getCustomerName() != null &&
                                    t.getCustomerName().toLowerCase().contains(searchTrim.toLowerCase());
                boolean matchDev  = t.getDeviceName() != null &&
                                    t.getDeviceName().toLowerCase().contains(searchTrim.toLowerCase());
                if (!matchId && !matchCust && !matchDev) return false;
            }
            return true;
        }).toList();

        return restrictToAssignedIfTechnician(filtered);
    }

    /**
     * Fetch a single non-deleted ticket.
     *
     * @throws ResponseStatusException 404 if not found or soft-deleted
     */
    @Transactional(readOnly = true)
    public RepairTicket getById(Long id) {
        RepairTicket ticket = ticketRepo.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        assertCanView(ticket);
        return ticket;
    }

    // ── Update ────────────────────────────────────────────────────────────

    /**
     * Full update of all editable ticket fields.
     *
     * @param id      the ticket to update
     * @param updated payload with new field values (bean-validated by the controller)
     * @return the saved ticket
     */
    public RepairTicket updateTicket(Long id, RepairTicket updated) {
        RepairTicket existing = getById(id);
        assertStaffOrAdmin();

        String oldStatus = existing.getStatus();
        List<String> changedFields = changedTicketFields(existing, updated);

        validateStatus(updated.getStatus());
        validatePriority(updated.getPriority());

        existing.setCustomerName(updated.getCustomerName());
        existing.setCustomerPhone(updated.getCustomerPhone());
        existing.setCustomerEmail(updated.getCustomerEmail());
        existing.setDeviceName(updated.getDeviceName());
        existing.setProblemDescription(updated.getProblemDescription());
        existing.setStatus(updated.getStatus());
        existing.setPriority(updated.getPriority());
        existing.setTargetCompletionDate(updated.getTargetCompletionDate());
        existing.setEstimatedCost(updated.getEstimatedCost());

        // technician handled separately via its own endpoint; ignore here
        RepairTicket saved = ticketRepo.save(existing);
        if (!changedFields.isEmpty()) {
            audit.record("TICKET_EDITED", "TICKET", id, "fields=" + String.join(",", changedFields));
        }
        if (!java.util.Objects.equals(oldStatus, saved.getStatus())) {
            audit.record("TICKET_STATUS_CHANGED", "TICKET", id,
                    "from=" + oldStatus + ";to=" + saved.getStatus());
        }
        return saved;
    }

    /**
     * Patch just the status field.
     *
     * @throws ResponseStatusException 400 for invalid status, 404 if ticket not found
     */
    public RepairTicket updateStatus(Long id, String status) {
        String upperStatus = status == null ? null : status.trim().toUpperCase();
        validateStatus(upperStatus);

        RepairTicket ticket = getById(id);
        String oldStatus = ticket.getStatus();
        SecurityUtils.currentUser()
                .filter(user -> user.getRole() == AppUser.Role.TECHNICIAN)
                .ifPresent(user -> {
                    if (!List.of("DIAGNOSED", "IN_REPAIR", "QUALITY_CHECK", "ON_HOLD", "READY")
                            .contains(upperStatus)) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Technicians may only update repair progress");
                    }
                });
        ticket.setStatus(upperStatus);
        RepairTicket saved = ticketRepo.save(ticket);
        if (!java.util.Objects.equals(oldStatus, upperStatus)) {
            audit.record("TICKET_STATUS_CHANGED", "TICKET", id, "from=" + oldStatus + ";to=" + upperStatus);
        }
        return saved;
    }

    /**
     * Assign or clear the assigned technician.
     *
     * @param technicianId null → unassign; non-null → assign the specified active technician
     */
    public RepairTicket assignTechnician(Long ticketId, Long technicianId) {
        RepairTicket ticket = getById(ticketId);
        assertStaffOrAdmin();
        Long oldTechnicianId = ticket.getTechnician() == null ? null : ticket.getTechnician().getId();

        if (technicianId == null) {
            ticket.setTechnician(null);
        } else {
            Technician tech = technicianRepo.findById(technicianId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
            if (!tech.isActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot assign an inactive technician");
            }
            ticket.setTechnician(tech);
        }
        RepairTicket saved = ticketRepo.save(ticket);
        Long newTechnicianId = saved.getTechnician() == null ? null : saved.getTechnician().getId();
        if (!java.util.Objects.equals(oldTechnicianId, newTechnicianId)) {
            audit.record("TICKET_TECHNICIAN_ASSIGNED", "TICKET", ticketId,
                    "fromTechnicianId=" + oldTechnicianId + ";toTechnicianId=" + newTechnicianId);
        }
        return saved;
    }

    // ── Delete ────────────────────────────────────────────────────────────

    /** Soft-delete: sets deleted = true; the row stays in MySQL. */
    public void softDelete(Long id) {
        assertAdmin();
        RepairTicket ticket = ticketRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        ticket.setDeleted(true);
        ticketRepo.save(ticket);
        audit.record("TICKET_ARCHIVED", "TICKET", id, "softDeleted=true");
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TicketStatsDto getStats() {
        List<RepairTicket> active = restrictToAssignedIfTechnician(
                ticketRepo.findByDeletedFalseOrderByCreatedAtDesc());

        long delivered = active.stream().filter(t -> "DELIVERED".equals(t.getStatus())).count();
        long cancelled = active.stream().filter(t -> "CANCELLED".equals(t.getStatus())).count();
        long total     = active.size();
        long open      = total - delivered - cancelled;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String s : STATUS_ORDER) {
            byStatus.put(s, active.stream().filter(t -> s.equals(t.getStatus())).count());
        }

        return new TicketStatsDto(total, open, delivered, cancelled, byStatus);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private void validateStatus(String status) {
        if (status == null || !RepairTicket.VALID_STATUSES.contains(status.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status '" + status + "'. Allowed values: " +
                    String.join(", ", STATUS_ORDER));
        }
    }

    private List<RepairTicket> restrictToAssignedIfTechnician(List<RepairTicket> tickets) {
        return SecurityUtils.currentUser()
                .filter(user -> user.getRole() == AppUser.Role.TECHNICIAN)
                .map(user -> tickets.stream().filter(t -> isAssignedTo(t, user)).toList())
                .orElse(tickets);
    }

    private void assertCanView(RepairTicket ticket) {
        SecurityUtils.currentUser().ifPresent(user -> {
            if (user.getRole() == AppUser.Role.TECHNICIAN && !isAssignedTo(ticket, user)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can only access tickets assigned to you");
            }
        });
    }

    private void assertStaffOrAdmin() {
        SecurityUtils.currentUser().ifPresent(user -> {
            if (user.getRole() == AppUser.Role.TECHNICIAN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Technicians may only update repair progress on assigned tickets");
            }
        });
    }

    private void assertAdmin() {
        SecurityUtils.currentUser().ifPresent(user -> {
            if (user.getRole() != AppUser.Role.ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only administrators can archive tickets");
            }
        });
    }

    private boolean isAssignedTo(RepairTicket ticket, AuthUserDetails user) {
        return ticket.getTechnician() != null
                && user.getTechnicianId() != null
                && ticket.getTechnician().getId().equals(user.getTechnicianId());
    }

    private void validatePriority(String priority) {
        if (priority == null || !RepairTicket.VALID_PRIORITIES.contains(priority.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid priority '" + priority + "'. Allowed values: HIGH, MEDIUM, LOW");
        }
    }

    private List<String> changedTicketFields(RepairTicket existing, RepairTicket updated) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        if (!java.util.Objects.equals(existing.getCustomerName(), updated.getCustomerName())) fields.add("customerName");
        if (!java.util.Objects.equals(existing.getCustomerPhone(), updated.getCustomerPhone())) fields.add("customerPhone");
        if (!java.util.Objects.equals(existing.getCustomerEmail(), updated.getCustomerEmail())) fields.add("customerEmail");
        if (!java.util.Objects.equals(existing.getDeviceName(), updated.getDeviceName())) fields.add("deviceName");
        if (!java.util.Objects.equals(existing.getProblemDescription(), updated.getProblemDescription())) fields.add("problemDescription");
        if (!java.util.Objects.equals(existing.getStatus(), updated.getStatus())) fields.add("status");
        if (!java.util.Objects.equals(existing.getPriority(), updated.getPriority())) fields.add("priority");
        if (!java.util.Objects.equals(existing.getTargetCompletionDate(), updated.getTargetCompletionDate())) fields.add("targetCompletionDate");
        if (!java.util.Objects.equals(existing.getEstimatedCost(), updated.getEstimatedCost())) fields.add("estimatedCost");
        return fields;
    }
}
