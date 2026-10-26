package FixFlow_backend;

import FixFlow_backend.model.AppUser;
import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.model.Technician;
import FixFlow_backend.repository.AppUserRepository;
import FixFlow_backend.repository.RepairTicketRepository;
import FixFlow_backend.repository.TechnicianRepository;
import FixFlow_backend.repository.InventoryItemRepository;
import FixFlow_backend.repository.InventoryMovementRepository;
import FixFlow_backend.repository.AuditEventRepository;
import FixFlow_backend.model.AuditEvent;
import FixFlow_backend.model.InventoryItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class AuthAccessIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AppUserRepository users;
    @Autowired TechnicianRepository technicians;
    @Autowired RepairTicketRepository tickets;
    @Autowired InventoryItemRepository inventory;
    @Autowired InventoryMovementRepository movements;
    @Autowired AuditEventRepository auditEvents;

    private Long assignedTicketId;
    private Long otherTicketId;
    private Long technicianRecordId;

    @BeforeEach
    void seed() {
        auditEvents.deleteAll();
        movements.deleteAll();
        tickets.deleteAll();
        inventory.deleteAll();
        users.deleteAll();
        technicians.deleteAll();

        Technician assigned = new Technician();
        assigned.setName("Alex Tech");
        assigned.setActive(true);
        assigned = technicians.save(assigned);
        technicianRecordId = assigned.getId();

        Technician otherTech = new Technician();
        otherTech.setName("Other Tech");
        otherTech.setActive(true);
        otherTech = technicians.save(otherTech);

        users.save(user("admin", "admin-pass", AppUser.Role.ADMIN, null, "Admin User"));
        users.save(user("staff", "staff-pass", AppUser.Role.SERVICE_STAFF, null, "Front Desk"));
        users.save(user("tech", "tech-pass", AppUser.Role.TECHNICIAN, assigned, "Alex Account"));

        RepairTicket mine = ticket("Assigned Customer", "Phone A");
        mine.setTechnician(assigned);
        assignedTicketId = tickets.save(mine).getId();

        RepairTicket theirs = ticket("Other Customer", "Phone B");
        theirs.setTechnician(otherTech);
        otherTicketId = tickets.save(theirs).getId();
    }

    @Test
    @DisplayName("login: valid credentials return a JWT and user profile")
    void login_validCredentials_returnsToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"admin-pass"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    @DisplayName("login: invalid credentials return 401")
    void login_invalidCredentials_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("unauthenticated requests return 401")
    void tickets_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("SERVICE_STAFF cannot manage users")
    void staff_cannotListUsers() throws Exception {
        String token = login("staff", "staff-pass");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN can list users")
    void admin_canListUsers() throws Exception {
        String token = login("admin", "admin-pass");
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("TECHNICIAN only sees assigned tickets")
    void technician_listsOnlyAssignedTickets() throws Exception {
        String token = login("tech", "tech-pass");
        mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(assignedTicketId));
    }

    @Test
    @DisplayName("TECHNICIAN is forbidden from another technician's ticket")
    void technician_cannotViewUnassignedTicket() throws Exception {
        String token = login("tech", "tech-pass");
        mockMvc.perform(get("/api/tickets/" + otherTicketId).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("TECHNICIAN can update status on an assigned ticket")
    void technician_canUpdateAssignedStatus() throws Exception {
        String token = login("tech", "tech-pass");
        mockMvc.perform(patch("/api/tickets/" + assignedTicketId + "/status")
                        .param("status", "IN_REPAIR")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REPAIR"));
        org.junit.jupiter.api.Assertions.assertTrue(auditEvents.findAll().stream().anyMatch(event ->
                event.getActionType().equals("TICKET_STATUS_CHANGED") && event.getActor().equals("tech")));
    }

    @Test
    @DisplayName("TECHNICIAN cannot complete or cancel a ticket")
    void technician_cannotSetTerminalStatus() throws Exception {
        String token = login("tech", "tech-pass");
        mockMvc.perform(patch("/api/tickets/" + assignedTicketId + "/status")
                        .param("status", "DELIVERED")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("TECHNICIAN cannot create tickets or reassign technicians")
    void technician_cannotCreateOrAssign() throws Exception {
        String token = login("tech", "tech-pass");
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"X","customerPhone":"1","deviceName":"Y","problemDescription":"Z"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/tickets/" + assignedTicketId + "/technician")
                        .param("technicianId", String.valueOf(technicianRecordId))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("inactive technicians cannot be assigned to tickets")
    void cannotAssignInactiveTechnician() throws Exception {
        Technician inactive = new Technician();
        inactive.setName("Inactive Tech");
        inactive.setActive(false);
        inactive = technicians.save(inactive);
        String token = login("admin", "admin-pass");
        mockMvc.perform(patch("/api/tickets/" + assignedTicketId + "/technician")
                        .param("technicianId", String.valueOf(inactive.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("tickets can be reassigned to an active technician")
    void canReassignToActiveTechnician() throws Exception {
        Technician replacement = new Technician();
        replacement.setName("Replacement Tech");
        replacement.setSkills("Board repair");
        replacement.setAvailability("AVAILABLE");
        replacement = technicians.save(replacement);
        String token = login("admin", "admin-pass");

        mockMvc.perform(patch("/api/tickets/" + assignedTicketId + "/technician")
                        .param("technicianId", String.valueOf(replacement.getId()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technician.name").value("Replacement Tech"));
        org.junit.jupiter.api.Assertions.assertTrue(auditEvents.findAll().stream().anyMatch(event ->
                event.getActionType().equals("TICKET_TECHNICIAN_ASSIGNED") && event.getActor().equals("admin")));
    }

    @Test
    @DisplayName("stock changes are audited and available to authorized staff")
    void stockRestockIsAudited() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Display Assembly");
        item.setSku("DSP-1");
        item.setQuantity(2);
        item.setReorderThreshold(1);
        Long itemId = inventory.save(item).getId();
        String token = login("staff", "staff-pass");

        mockMvc.perform(post("/api/inventory/" + itemId + "/stock-adjustments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantityChange\":3,\"note\":\"Supplier delivery\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(5));
        mockMvc.perform(get("/api/inventory/" + itemId + "/movements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].performedBy").value("staff"))
                .andExpect(jsonPath("$[0].quantityChange").value(3));
        org.junit.jupiter.api.Assertions.assertTrue(auditEvents.findAll().stream().anyMatch(event ->
                event.getActionType().equals("INVENTORY_RESTOCKED") && event.getActor().equals("staff")));
    }

    @Test
    @DisplayName("part consumption refuses insufficient stock and leaves stock unchanged")
    void insufficientStockIsRejected() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Battery");
        item.setQuantity(1);
        Long itemId = inventory.save(item).getId();
        String token = login("staff", "staff-pass");

        mockMvc.perform(post("/api/tickets/" + assignedTicketId + "/parts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":%d,\"quantity\":2}".formatted(itemId)))
                .andExpect(status().isBadRequest());
        org.junit.jupiter.api.Assertions.assertEquals(1, inventory.findById(itemId).orElseThrow().getQuantity());
        org.junit.jupiter.api.Assertions.assertTrue(movements.findByTicket_IdOrderByCreatedAtDesc(assignedTicketId).isEmpty());
    }

    @Test
    @DisplayName("part consumption updates stock and records ticket and user")
    void consumePartUpdatesStockAndAudit() throws Exception {
        InventoryItem item = new InventoryItem();
        item.setName("Charging Port");
        item.setQuantity(4);
        Long itemId = inventory.save(item).getId();
        String token = login("staff", "staff-pass");

        mockMvc.perform(post("/api/tickets/" + assignedTicketId + "/parts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":%d,\"quantity\":2,\"note\":\"Replaced port\"}".formatted(itemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2));

        mockMvc.perform(get("/api/inventory/tickets/" + assignedTicketId + "/movements")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CONSUMPTION"))
                .andExpect(jsonPath("$[0].performedBy").value("staff"))
                .andExpect(jsonPath("$[0].ticketId").value(assignedTicketId));
        org.junit.jupiter.api.Assertions.assertTrue(auditEvents.findAll().stream().anyMatch(event ->
                event.getActionType().equals("INVENTORY_CONSUMED") && event.getActor().equals("staff")));
    }

    @Test
    @DisplayName("audit trail is available to administrators and not to ordinary staff")
    void auditEventsAreAdminReadOnly() throws Exception {
        String staffToken = login("staff", "staff-pass");
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Audit Customer\",\"customerPhone\":\"555\",\"deviceName\":\"Tablet\",\"problemDescription\":\"Screen repair\",\"status\":\"RECEIVED\",\"priority\":\"HIGH\"}"))
                .andExpect(status().isCreated());

        AuditEvent created = auditEvents.findTop200ByOrderByOccurredAtDesc().stream()
                .filter(event -> event.getActionType().equals("TICKET_CREATED"))
                .findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("staff", created.getActor());
        org.junit.jupiter.api.Assertions.assertFalse(created.getDetails().contains("Audit Customer"));

        mockMvc.perform(get("/api/audit-events").header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());
        String adminToken = login("admin", "admin-pass");
        mockMvc.perform(get("/api/audit-events").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("TICKET_CREATED"));
    }

    @Test
    @DisplayName("SERVICE_STAFF can create a ticket")
    void staff_canCreateTicket() throws Exception {
        String token = login("staff", "staff-pass");
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerName":"Pat","customerPhone":"555","deviceName":"Laptop","problemDescription":"Fan noise","status":"RECEIVED","priority":"LOW"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Pat"));
    }

    @Test
    @DisplayName("ticket creation rejects missing required fields")
    void ticketCreation_missingRequiredFields_returnsBadRequest() throws Exception {
        String token = login("staff", "staff-pass");
        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"Pat\",\"customerPhone\":\"555\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("logout revokes the token")
    void logout_revokesToken() throws Exception {
        String token = login("admin", "admin-pass");
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    private AppUser user(String username, String password, AppUser.Role role, Technician technician, String name) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setTechnician(technician);
        user.setFullName(name);
        user.setEnabled(true);
        return user;
    }

    private RepairTicket ticket(String customer, String device) {
        RepairTicket t = new RepairTicket();
        t.setCustomerName(customer);
        t.setCustomerPhone("9999900000");
        t.setDeviceName(device);
        t.setProblemDescription("Needs repair");
        t.setStatus("RECEIVED");
        t.setPriority("MEDIUM");
        t.setDeleted(false);
        return t;
    }
}
