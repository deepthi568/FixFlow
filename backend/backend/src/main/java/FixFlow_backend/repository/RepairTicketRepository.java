package FixFlow_backend.repository;

import FixFlow_backend.model.RepairTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairTicketRepository extends JpaRepository<RepairTicket, Long> {

    /** All non-deleted tickets, newest first – the default list view. */
    List<RepairTicket> findByDeletedFalseOrderByCreatedAtDesc();

    /** Filter by exact status, non-deleted only. */
    List<RepairTicket> findByStatusAndDeletedFalse(String status);

    /** Search by customer name (case-insensitive substring), non-deleted only. */
    List<RepairTicket> findByCustomerNameContainingIgnoreCaseAndDeletedFalse(String customerName);

    /** Combined search + status filter, non-deleted only. */
    List<RepairTicket> findByCustomerNameContainingIgnoreCaseAndStatusAndDeletedFalse(
            String customerName, String status);
}
