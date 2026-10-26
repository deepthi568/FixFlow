package FixFlow_backend.controller;

import FixFlow_backend.dto.InventoryUsageRequest;
import FixFlow_backend.model.InventoryItem;
import FixFlow_backend.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/{ticketId}/parts")
public class TicketPartsController {
    private final InventoryService inventory;

    public TicketPartsController(InventoryService inventory) { this.inventory = inventory; }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public InventoryItem consume(@PathVariable Long ticketId, @Valid @RequestBody InventoryUsageRequest request) {
        return inventory.consume(ticketId, request.getItemId(), request.getQuantity(), request.getNote());
    }
}
