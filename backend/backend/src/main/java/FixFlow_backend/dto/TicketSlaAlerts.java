package FixFlow_backend.dto;

import java.time.LocalDate;
import java.util.List;

public record TicketSlaAlerts(LocalDate asOfDate, int approachingDays,
                              List<TicketSlaItem> overdue, List<TicketSlaItem> approaching) {}
