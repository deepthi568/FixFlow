package FixFlow_backend.repository;

import FixFlow_backend.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    List<InventoryMovement> findByItem_IdOrderByCreatedAtDesc(Long itemId);
    List<InventoryMovement> findByTicket_IdOrderByCreatedAtDesc(Long ticketId);
}
