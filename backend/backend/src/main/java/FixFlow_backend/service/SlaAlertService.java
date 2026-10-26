package FixFlow_backend.service;

import FixFlow_backend.dto.TicketSlaAlerts;
import FixFlow_backend.dto.TicketSlaItem;
import FixFlow_backend.model.AppUser;
import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.repository.RepairTicketRepository;
import FixFlow_backend.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class SlaAlertService {
    private static final Set<String> TERMINAL_STATUSES = Set.of("DELIVERED", "CANCELLED");

    private final RepairTicketRepository tickets;
    private final int approachingDays;

    public SlaAlertService(RepairTicketRepository tickets,
                           @Value("${fixflow.sla.approaching-days:2}") int approachingDays) {
        if (approachingDays < 0) throw new IllegalArgumentException("SLA approaching days cannot be negative");
        this.tickets = tickets;
        this.approachingDays = approachingDays;
    }

    @Transactional(readOnly = true)
    public TicketSlaAlerts getAlerts() {
        List<RepairTicket> active = tickets.findByDeletedFalseOrderByCreatedAtDesc();
        var currentUser = SecurityUtils.currentUser();
        if (currentUser.filter(user -> user.getRole() == AppUser.Role.TECHNICIAN).isPresent()) {
            Long technicianId = currentUser.get().getTechnicianId();
            active = active.stream().filter(ticket -> ticket.getTechnician() != null && technicianId != null
                    && ticket.getTechnician().getId().equals(technicianId)).toList();
        }
        return calculate(active, LocalDate.now(), approachingDays);
    }

    public static TicketSlaAlerts calculate(List<RepairTicket> records, LocalDate today, int approachingDays) {
        LocalDate deadline = today.plusDays(Math.max(0, approachingDays));
        List<TicketSlaItem> overdue = records.stream()
                .filter(SlaAlertService::isNonTerminal)
                .filter(ticket -> ticket.getTargetCompletionDate() != null && ticket.getTargetCompletionDate().isBefore(today))
                .map(TicketSlaItem::from).toList();
        List<TicketSlaItem> approaching = records.stream()
                .filter(SlaAlertService::isNonTerminal)
                .filter(ticket -> ticket.getTargetCompletionDate() != null
                        && !ticket.getTargetCompletionDate().isBefore(today)
                        && !ticket.getTargetCompletionDate().isAfter(deadline))
                .map(TicketSlaItem::from).toList();
        return new TicketSlaAlerts(today, approachingDays, overdue, approaching);
    }

    private static boolean isNonTerminal(RepairTicket ticket) {
        return !TERMINAL_STATUSES.contains(ticket.getStatus());
    }
}
