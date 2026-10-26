package FixFlow_backend.dto;

import FixFlow_backend.model.RepairTicket;

import java.time.LocalDate;

public record TicketSlaItem(Long ticketId, String deviceName, String status, String priority,
                            LocalDate targetCompletionDate, String technicianName) {
    public static TicketSlaItem from(RepairTicket ticket) {
        return new TicketSlaItem(ticket.getId(), ticket.getDeviceName(), ticket.getStatus(), ticket.getPriority(),
                ticket.getTargetCompletionDate(), ticket.getTechnician() == null ? null : ticket.getTechnician().getName());
    }
}
