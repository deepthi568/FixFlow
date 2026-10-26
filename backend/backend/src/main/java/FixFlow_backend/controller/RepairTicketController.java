package FixFlow_backend.controller;

import FixFlow_backend.dto.TicketStatsDto;
import FixFlow_backend.dto.TicketSlaAlerts;
import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.service.RepairTicketService;
import FixFlow_backend.service.SlaAlertService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for repair tickets.
 *
 * <p>Stays thin — all business logic lives in {@link RepairTicketService}.</p>
 *
 * <p>All endpoints are under {@code /api/tickets}.</p>
 */
@RestController
@RequestMapping("/api/tickets")
public class RepairTicketController {

    private final RepairTicketService service;
    private final SlaAlertService slaAlerts;

    public RepairTicketController(RepairTicketService service, SlaAlertService slaAlerts) {
        this.service = service;
        this.slaAlerts = slaAlerts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public RepairTicket createTicket(@Valid @RequestBody RepairTicket ticket) {
        return service.createTicket(ticket);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public List<RepairTicket> getAllTickets(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return service.listTickets(search, status, priority);
    }

    /**
     * Declared before {@code /{id}} so "stats" is not treated as an identifier.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public TicketStatsDto getStats() {
        return service.getStats();
    }

    @GetMapping("/sla-alerts")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public TicketSlaAlerts getSlaAlerts() { return slaAlerts.getAlerts(); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public RepairTicket getTicketById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public RepairTicket updateTicket(
            @PathVariable Long id,
            @Valid @RequestBody RepairTicket ticket) {
        return service.updateTicket(id, ticket);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public RepairTicket updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return service.updateStatus(id, status);
    }

    @PatchMapping("/{id}/technician")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public RepairTicket assignTechnician(
            @PathVariable Long id,
            @RequestParam(required = false) Long technicianId) {
        return service.assignTechnician(id, technicianId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTicket(@PathVariable Long id) {
        service.softDelete(id);
    }
}
