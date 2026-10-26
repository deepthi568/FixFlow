package FixFlow_backend.service;

import FixFlow_backend.dto.InventoryMovementResponse;
import FixFlow_backend.model.AppUser;
import FixFlow_backend.model.InventoryItem;
import FixFlow_backend.model.InventoryMovement;
import FixFlow_backend.model.RepairTicket;
import FixFlow_backend.repository.InventoryItemRepository;
import FixFlow_backend.repository.InventoryMovementRepository;
import FixFlow_backend.repository.RepairTicketRepository;
import FixFlow_backend.security.AuthUserDetails;
import FixFlow_backend.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class InventoryService {
    private final InventoryItemRepository items;
    private final InventoryMovementRepository movements;
    private final RepairTicketRepository tickets;
    private final AuditService audit;

    public InventoryService(InventoryItemRepository items, InventoryMovementRepository movements,
                            RepairTicketRepository tickets, AuditService audit) {
        this.items = items;
        this.movements = movements;
        this.tickets = tickets;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> list() {
        return items.findAllByOrderByNameAsc().stream().filter(InventoryItem::isActive).toList();
    }

    @Transactional(readOnly = true)
    public InventoryItem get(Long id) {
        return items.findById(id).filter(InventoryItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    }

    @Transactional(readOnly = true)
    public List<InventoryItem> lowStock() {
        return list().stream().filter(i -> i.getQuantity() <= i.getReorderThreshold()).toList();
    }

    public InventoryItem create(InventoryItem item) {
        validateThreshold(item.getReorderThreshold());
        item.setActive(true);
        item.setCreatedBy(currentActor());
        item.setUpdatedBy(currentActor());
        InventoryItem saved = items.save(item);
        record(saved, null, saved.getQuantity(), InventoryMovement.Type.CREATED, "Initial stock");
        audit.record("INVENTORY_ITEM_CREATED", "INVENTORY_ITEM", saved.getId(),
                "initialQuantity=" + saved.getQuantity() + ";reorderThreshold=" + saved.getReorderThreshold());
        return saved;
    }

    public InventoryItem update(Long id, InventoryItem updated) {
        InventoryItem existing = lockActive(id);
        validateThreshold(updated.getReorderThreshold());
        int delta = updated.getQuantity() - existing.getQuantity();
        java.util.List<String> changedFields = changedItemFields(existing, updated);
        if (existing.getQuantity() + delta < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }
        existing.setName(updated.getName());
        existing.setSku(updated.getSku());
        existing.setDescription(updated.getDescription());
        existing.setQuantity(updated.getQuantity());
        existing.setReorderThreshold(updated.getReorderThreshold());
        existing.setUnit(updated.getUnit());
        existing.setNotes(updated.getNotes());
        existing.setUpdatedBy(currentActor());
        InventoryItem saved = items.save(existing);
        if (delta != 0) record(saved, null, delta, InventoryMovement.Type.ADJUSTMENT, "Stock changed while editing part");
        if (!changedFields.isEmpty()) {
            audit.record("INVENTORY_ITEM_UPDATED", "INVENTORY_ITEM", id,
                    "fields=" + String.join(",", changedFields) + ";stockDelta=" + delta);
        }
        return saved;
    }

    public InventoryItem adjust(Long id, int delta, String note) {
        if (delta == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity change cannot be zero");
        InventoryItem item = lockActive(id);
        long adjustedQuantity = (long) item.getQuantity() + delta;
        if (adjustedQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock; stock cannot become negative");
        }
        if (adjustedQuantity > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock quantity exceeds the supported range");
        }
        item.setQuantity((int) adjustedQuantity);
        item.setUpdatedBy(currentActor());
        InventoryItem saved = items.save(item);
        record(saved, null, delta, delta > 0 ? InventoryMovement.Type.RESTOCK : InventoryMovement.Type.ADJUSTMENT, note);
        audit.record(delta > 0 ? "INVENTORY_RESTOCKED" : "INVENTORY_STOCK_ADJUSTED",
                "INVENTORY_ITEM", id, "quantityChange=" + delta);
        return saved;
    }

    public InventoryItem consume(Long ticketId, Long itemId, int quantity, String note) {
        if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        RepairTicket ticket = tickets.findById(ticketId)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        assertCanUseParts(ticket);
        InventoryItem item = lockActive(itemId);
        if (item.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for this part");
        }
        item.setQuantity(item.getQuantity() - quantity);
        item.setUpdatedBy(currentActor());
        InventoryItem saved = items.save(item);
        record(saved, ticket, -quantity, InventoryMovement.Type.CONSUMPTION, note);
        audit.record("INVENTORY_CONSUMED", "INVENTORY_ITEM", itemId,
                "ticketId=" + ticketId + ";quantity=" + quantity);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> history(Long itemId) {
        get(itemId);
        return movements.findByItem_IdOrderByCreatedAtDesc(itemId).stream()
                .map(InventoryMovementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InventoryMovementResponse> ticketUsage(Long ticketId) {
        tickets.findById(ticketId).filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        return movements.findByTicket_IdOrderByCreatedAtDesc(ticketId).stream()
                .map(InventoryMovementResponse::from).toList();
    }

    public void deactivate(Long id) {
        InventoryItem item = lockActive(id);
        item.setActive(false);
        item.setUpdatedBy(currentActor());
        items.save(item);
        audit.record("INVENTORY_ITEM_DEACTIVATED", "INVENTORY_ITEM", id, "active=false");
    }

    private InventoryItem lockActive(Long id) {
        return items.findByIdForUpdate(id).filter(InventoryItem::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory item not found"));
    }

    private void record(InventoryItem item, RepairTicket ticket, int delta,
                        InventoryMovement.Type type, String note) {
        AuthUserDetails actor = SecurityUtils.currentUser().orElse(null);
        movements.save(new InventoryMovement(item, ticket, delta, type,
                actor == null ? "system" : actor.getUsername(), note));
    }

    private String currentActor() {
        return SecurityUtils.currentUser().map(AuthUserDetails::getUsername).orElse("system");
    }

    private void assertCanUseParts(RepairTicket ticket) {
        AuthUserDetails user = SecurityUtils.requireUser();
        if (user.getRole() == AppUser.Role.TECHNICIAN
                && (ticket.getTechnician() == null || user.getTechnicianId() == null
                || !ticket.getTechnician().getId().equals(user.getTechnicianId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only record parts for assigned tickets");
        }
    }

    private void validateThreshold(int threshold) {
        if (threshold < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder threshold cannot be negative");
    }

    private java.util.List<String> changedItemFields(InventoryItem existing, InventoryItem updated) {
        java.util.ArrayList<String> fields = new java.util.ArrayList<>();
        if (!java.util.Objects.equals(existing.getName(), updated.getName())) fields.add("name");
        if (!java.util.Objects.equals(existing.getSku(), updated.getSku())) fields.add("sku");
        if (!java.util.Objects.equals(existing.getDescription(), updated.getDescription())) fields.add("description");
        if (existing.getQuantity() != updated.getQuantity()) fields.add("quantity");
        if (existing.getReorderThreshold() != updated.getReorderThreshold()) fields.add("reorderThreshold");
        if (!java.util.Objects.equals(existing.getUnit(), updated.getUnit())) fields.add("unit");
        if (!java.util.Objects.equals(existing.getNotes(), updated.getNotes())) fields.add("notes");
        return fields;
    }
}
