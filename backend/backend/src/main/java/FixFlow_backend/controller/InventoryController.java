package FixFlow_backend.controller;

import FixFlow_backend.model.InventoryItem;
import FixFlow_backend.dto.InventoryMovementResponse;
import FixFlow_backend.dto.StockAdjustmentRequest;
import FixFlow_backend.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public List<InventoryItem> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public InventoryItem get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public List<InventoryItem> lowStock() { return service.lowStock(); }

    @GetMapping("/{id}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public List<InventoryMovementResponse> history(@PathVariable Long id) { return service.history(id); }

    @GetMapping("/tickets/{ticketId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF','TECHNICIAN')")
    public List<InventoryMovementResponse> ticketUsage(@PathVariable Long ticketId) {
        return service.ticketUsage(ticketId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public InventoryItem create(@Valid @RequestBody InventoryItem item) {
        return service.create(item);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public InventoryItem update(@PathVariable Long id, @Valid @RequestBody InventoryItem updated) {
        return service.update(id, updated);
    }

    @PostMapping("/{id}/stock-adjustments")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public InventoryItem adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustmentRequest request) {
        return service.adjust(id, request.getQuantityChange(), request.getNote());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) {
        service.deactivate(id);
    }
}
