package FixFlow_backend.dto;

import FixFlow_backend.model.InventoryMovement;

import java.time.LocalDateTime;

public record InventoryMovementResponse(Long id, Long itemId, String itemName, Long ticketId,
                                        int quantityChange, String type, String performedBy,
                                        String note, LocalDateTime createdAt) {
    public static InventoryMovementResponse from(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(), movement.getItem().getId(), movement.getItem().getName(),
                movement.getTicket() == null ? null : movement.getTicket().getId(),
                movement.getQuantityChange(), movement.getType().name(), movement.getPerformedBy(),
                movement.getNote(), movement.getCreatedAt());
    }
}
