package FixFlow_backend.repository;

import FixFlow_backend.model.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    /** Active technicians only, sorted alphabetically – used to populate the assignment dropdown. */
    List<Technician> findByActiveTrueOrderByNameAsc();

    List<Technician> findAllByOrderByNameAsc();
}
