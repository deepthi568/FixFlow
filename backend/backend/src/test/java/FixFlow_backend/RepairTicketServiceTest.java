package FixFlow_backend;

import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.repository.RepairTicketRepository;
import FixFlow_backend.repository.TechnicianRepository;
import FixFlow_backend.service.RepairTicketService;
import FixFlow_backend.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration-style tests for {@link RepairTicketService} running against
 * an H2 in-memory database (no MySQL required).
 *
 * <p>{@code @DataJpaTest} loads only the JPA slice and replaces the data source
 * with an embedded H2 database. {@code @Import(RepairTicketService.class)} wires
 * the service under test into the context.</p>
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({RepairTicketService.class, AuditService.class})
class RepairTicketServiceTest {

    @Autowired RepairTicketService       service;
    @Autowired RepairTicketRepository    ticketRepo;
    @Autowired TechnicianRepository      technicianRepo;

    // ── Helpers ───────────────────────────────────────────────────────────

    private RepairTicket buildTicket(String customerName, String device,
                                     String status, String priority) {
        RepairTicket t = new RepairTicket();
        t.setCustomerName(customerName);
        t.setCustomerPhone("9999900000");
        t.setDeviceName(device);
        t.setProblemDescription("Test fault description");
        t.setStatus(status);
        t.setPriority(priority);
        return t;
    }

    @BeforeEach
    void setUp() {
        ticketRepo.deleteAll();
    }

    // ── Create ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTicket: saves a valid ticket and returns it with an assigned ID")
    void createTicket_validPayload_returnsPersistedTicket() {
        RepairTicket ticket = buildTicket("Alice", "Dell XPS", "RECEIVED", "HIGH");
        ticket.setEstimatedCost(new BigDecimal("1500.00"));
        ticket.setTargetCompletionDate(LocalDate.now().plusDays(7));

        RepairTicket saved = service.createTicket(ticket);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerName()).isEqualTo("Alice");
        assertThat(saved.getDeviceName()).isEqualTo("Dell XPS");
        assertThat(saved.getStatus()).isEqualTo("RECEIVED");
        assertThat(saved.getPriority()).isEqualTo("HIGH");
        assertThat(saved.getEstimatedCost()).isEqualByComparingTo("1500.00");
        assertThat(saved.isDeleted()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("createTicket: defaults status to RECEIVED and priority to MEDIUM")
    void createTicket_defaultsApplied() {
        RepairTicket ticket = new RepairTicket();
        ticket.setCustomerName("Bob");
        ticket.setCustomerPhone("9000000001");
        ticket.setDeviceName("HP Pavilion");
        ticket.setProblemDescription("Screen flicker");
        // status and priority use entity field defaults

        RepairTicket saved = service.createTicket(ticket);

        assertThat(saved.getStatus()).isEqualTo("RECEIVED");
        assertThat(saved.getPriority()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("createTicket: rejects an invalid status with HTTP 400")
    void createTicket_invalidStatus_throws400() {
        RepairTicket ticket = buildTicket("Charlie", "MacBook", "FLYING", "LOW");

        assertThatThrownBy(() -> service.createTicket(ticket))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                    assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("createTicket: rejects an invalid priority with HTTP 400")
    void createTicket_invalidPriority_throws400() {
        RepairTicket ticket = buildTicket("Diana", "iPad", "RECEIVED", "URGENT");

        assertThatThrownBy(() -> service.createTicket(ticket))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                    assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    // ── Retrieve ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getById: returns an existing ticket by ID")
    void getById_existingTicket_returnsIt() {
        RepairTicket saved = service.createTicket(buildTicket("Eve", "iPhone", "RECEIVED", "MEDIUM"));

        RepairTicket found = service.getById(saved.getId());

        assertThat(found.getCustomerName()).isEqualTo("Eve");
    }

    @Test
    @DisplayName("getById: throws 404 for a non-existent ticket")
    void getById_nonExistent_throws404() {
        assertThatThrownBy(() -> service.getById(99999L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                    assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    @DisplayName("getById: throws 404 for a soft-deleted ticket")
    void getById_softDeleted_throws404() {
        RepairTicket saved = service.createTicket(buildTicket("Frank", "Samsung", "RECEIVED", "LOW"));
        service.softDelete(saved.getId());

        assertThatThrownBy(() -> service.getById(saved.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                    assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    // ── List / Search ─────────────────────────────────────────────────────

    @Test
    @DisplayName("listTickets: soft-deleted tickets are excluded from the list")
    void listTickets_excludesSoftDeleted() {
        service.createTicket(buildTicket("Grace", "Lenovo", "RECEIVED", "HIGH"));
        RepairTicket toDelete = service.createTicket(buildTicket("Henry", "Asus", "RECEIVED", "LOW"));
        service.softDelete(toDelete.getId());

        List<RepairTicket> result = service.listTickets(null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Grace");
    }

    @Test
    @DisplayName("listTickets: search by customer name (case-insensitive)")
    void listTickets_searchByCustomerName() {
        service.createTicket(buildTicket("Grace", "Lenovo", "RECEIVED", "HIGH"));
        service.createTicket(buildTicket("Henry", "Asus",   "IN_REPAIR", "LOW"));

        List<RepairTicket> result = service.listTickets("grace", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Grace");
    }

    @Test
    @DisplayName("listTickets: search by device name (case-insensitive)")
    void listTickets_searchByDeviceName() {
        service.createTicket(buildTicket("Ivan", "Dell XPS 15", "RECEIVED", "MEDIUM"));
        service.createTicket(buildTicket("Judy", "HP Envy",     "RECEIVED", "MEDIUM"));

        List<RepairTicket> result = service.listTickets("dell", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDeviceName()).contains("Dell");
    }

    @Test
    @DisplayName("listTickets: search by ticket ID")
    void listTickets_searchById() {
        RepairTicket t1 = service.createTicket(buildTicket("Karl",  "Nokia", "RECEIVED", "LOW"));
                          service.createTicket(buildTicket("Linda", "Sony",  "RECEIVED", "LOW"));

        List<RepairTicket> result = service.listTickets(String.valueOf(t1.getId()), null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(t1.getId());
    }

    @Test
    @DisplayName("listTickets: filter by status")
    void listTickets_filterByStatus() {
        service.createTicket(buildTicket("Mike", "iPad",  "RECEIVED",  "HIGH"));
        service.createTicket(buildTicket("Nina", "Pixel", "DELIVERED", "LOW"));

        List<RepairTicket> result = service.listTickets(null, "DELIVERED", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Nina");
    }

    @Test
    @DisplayName("listTickets: filter by priority")
    void listTickets_filterByPriority() {
        service.createTicket(buildTicket("Oscar", "Galaxy", "RECEIVED", "HIGH"));
        service.createTicket(buildTicket("Pam",   "Pixel",  "RECEIVED", "LOW"));

        List<RepairTicket> result = service.listTickets(null, null, "HIGH");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Oscar");
    }

    @Test
    @DisplayName("listTickets: combined search + status + priority filters")
    void listTickets_combinedFilters() {
        service.createTicket(buildTicket("Quinn", "Dell", "IN_REPAIR", "HIGH"));
        service.createTicket(buildTicket("Quinn", "Dell", "RECEIVED",  "LOW"));
        service.createTicket(buildTicket("Rob",   "Dell", "IN_REPAIR", "HIGH"));

        List<RepairTicket> result = service.listTickets("quinn", "IN_REPAIR", "HIGH");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCustomerName()).isEqualTo("Quinn");
        assertThat(result.get(0).getStatus()).isEqualTo("IN_REPAIR");
    }

    // ── Status Update ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateStatus: transitions ticket to a valid new status")
    void updateStatus_validTransition_savesNewStatus() {
        RepairTicket saved = service.createTicket(buildTicket("Sara", "Acer", "RECEIVED", "MEDIUM"));

        RepairTicket updated = service.updateStatus(saved.getId(), "IN_REPAIR");

        assertThat(updated.getStatus()).isEqualTo("IN_REPAIR");
    }

    @Test
    @DisplayName("updateStatus: rejects invalid status value with HTTP 400")
    void updateStatus_invalidStatus_throws400() {
        RepairTicket saved = service.createTicket(buildTicket("Tom", "Toshiba", "RECEIVED", "LOW"));

        assertThatThrownBy(() -> service.updateStatus(saved.getId(), "BROKEN"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex ->
                    assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
    }

    @Test
    @DisplayName("updateStatus: all 8 valid statuses are accepted")
    void updateStatus_allValidStatuses_accepted() {
        RepairTicket saved = service.createTicket(buildTicket("Uma", "Surface", "RECEIVED", "LOW"));
        Long id = saved.getId();

        for (String s : List.of("DIAGNOSED", "IN_REPAIR", "QUALITY_CHECK",
                                 "READY", "DELIVERED", "ON_HOLD", "CANCELLED")) {
            RepairTicket r = service.updateStatus(id, s);
            assertThat(r.getStatus()).isEqualTo(s);
        }
    }

    // ── Update (PUT) ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTicket: updates all editable fields")
    void updateTicket_updatesFields() {
        RepairTicket saved = service.createTicket(buildTicket("Vera", "Huawei", "RECEIVED", "LOW"));

        RepairTicket patch = buildTicket("Vera Updated", "Huawei P50", "DIAGNOSED", "HIGH");
        patch.setEstimatedCost(new BigDecimal("2500.00"));
        patch.setTargetCompletionDate(LocalDate.now().plusDays(3));

        RepairTicket result = service.updateTicket(saved.getId(), patch);

        assertThat(result.getCustomerName()).isEqualTo("Vera Updated");
        assertThat(result.getDeviceName()).isEqualTo("Huawei P50");
        assertThat(result.getStatus()).isEqualTo("DIAGNOSED");
        assertThat(result.getPriority()).isEqualTo("HIGH");
        assertThat(result.getEstimatedCost()).isEqualByComparingTo("2500.00");
        assertThat(result.getTargetCompletionDate()).isEqualTo(LocalDate.now().plusDays(3));
    }

    // ── Soft Delete ───────────────────────────────────────────────────────

    @Test
    @DisplayName("softDelete: marks ticket as deleted without removing the row")
    void softDelete_setsDeletedFlagAndExcludesFromList() {
        RepairTicket saved = service.createTicket(buildTicket("Wanda", "Pixel 8", "RECEIVED", "MEDIUM"));
        Long id = saved.getId();

        service.softDelete(id);

        // Row still exists in the database
        assertThat(ticketRepo.findById(id)).isPresent();
        assertThat(ticketRepo.findById(id).get().isDeleted()).isTrue();

        // But the list and getById exclude it
        assertThat(service.listTickets(null, null, null)).isEmpty();
    }

    // ── Stats ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats: counts are accurate and soft-deleted tickets are excluded")
    void getStats_countsAreCorrect() {
        service.createTicket(buildTicket("X1", "Device", "RECEIVED",  "LOW"));
        service.createTicket(buildTicket("X2", "Device", "DELIVERED", "LOW"));
        service.createTicket(buildTicket("X3", "Device", "CANCELLED", "LOW"));
        RepairTicket toDelete = service.createTicket(buildTicket("X4", "Device", "RECEIVED", "LOW"));
        service.softDelete(toDelete.getId());

        var stats = service.getStats();

        // Soft-deleted X4 must NOT be counted
        assertThat(stats.getTotal()).isEqualTo(3);
        assertThat(stats.getDelivered()).isEqualTo(1);
        assertThat(stats.getCancelled()).isEqualTo(1);
        assertThat(stats.getOpen()).isEqualTo(1);   // total - delivered - cancelled
        assertThat(stats.getByStatus().get("RECEIVED")).isEqualTo(1);
        assertThat(stats.getByStatus().get("DELIVERED")).isEqualTo(1);
        assertThat(stats.getByStatus().get("CANCELLED")).isEqualTo(1);
    }
}
