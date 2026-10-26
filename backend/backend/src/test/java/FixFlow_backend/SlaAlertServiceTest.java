package FixFlow_backend;

import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.service.SlaAlertService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SlaAlertServiceTest {
    @Test
    void alertsRespectDeadlineWindowAndIgnoreTerminalTickets() {
        LocalDate today = LocalDate.of(2026, 10, 26);
        RepairTicket overdue = ticket("IN_REPAIR", today.minusDays(1));
        RepairTicket dueToday = ticket("RECEIVED", today);
        RepairTicket dueAtThreshold = ticket("READY", today.plusDays(2));
        RepairTicket outsideThreshold = ticket("DIAGNOSED", today.plusDays(3));
        RepairTicket deliveredLate = ticket("DELIVERED", today.minusDays(3));
        RepairTicket noDeadline = ticket("IN_REPAIR", null);

        var alerts = SlaAlertService.calculate(
                List.of(overdue, dueToday, dueAtThreshold, outsideThreshold, deliveredLate, noDeadline), today, 2);

        assertEquals(List.of("IN_REPAIR"), alerts.overdue().stream().map(item -> item.status()).toList());
        assertEquals(List.of("RECEIVED", "READY"), alerts.approaching().stream().map(item -> item.status()).toList());
        assertEquals(2, alerts.approachingDays());
        assertEquals(today, alerts.asOfDate());
    }

    private RepairTicket ticket(String status, LocalDate target) {
        RepairTicket ticket = new RepairTicket();
        ticket.setStatus(status);
        ticket.setTargetCompletionDate(target);
        ticket.setDeleted(false);
        return ticket;
    }
}
