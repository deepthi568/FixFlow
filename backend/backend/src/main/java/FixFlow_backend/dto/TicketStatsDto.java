package FixFlow_backend.dto;

import java.util.Map;

/** Read-only snapshot used by GET /api/tickets/stats. */
public class TicketStatsDto {

    private final long total;
    private final long open;
    private final long delivered;
    private final long cancelled;
    private final Map<String, Long> byStatus;

    public TicketStatsDto(long total, long open, long delivered, long cancelled, Map<String, Long> byStatus) {
        this.total = total;
        this.open = open;
        this.delivered = delivered;
        this.cancelled = cancelled;
        this.byStatus = byStatus;
    }

    public long getTotal()                  { return total; }
    public long getOpen()                   { return open; }
    public long getDelivered()              { return delivered; }
    public long getCancelled()              { return cancelled; }
    public Map<String, Long> getByStatus()  { return byStatus; }
}
