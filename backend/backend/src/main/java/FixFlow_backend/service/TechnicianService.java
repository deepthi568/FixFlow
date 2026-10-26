package FixFlow_backend.service;

import FixFlow_backend.model.Technician;
import FixFlow_backend.repository.TechnicianRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
@Transactional
public class TechnicianService {
    private static final Set<String> AVAILABILITIES = Set.of("AVAILABLE", "BUSY", "UNAVAILABLE");
    private final TechnicianRepository technicians;

    public TechnicianService(TechnicianRepository technicians) { this.technicians = technicians; }

    @Transactional(readOnly = true)
    public List<Technician> list(boolean includeInactive) {
        return includeInactive ? technicians.findAllByOrderByNameAsc() : technicians.findByActiveTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Technician get(Long id) {
        return technicians.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
    }

    public Technician create(Technician technician) {
        validateAvailability(technician.getAvailability());
        technician.setActive(true);
        if (technician.getAvailability() == null || technician.getAvailability().isBlank()) {
            technician.setAvailability("AVAILABLE");
        } else {
            technician.setAvailability(technician.getAvailability().trim().toUpperCase());
        }
        return technicians.save(technician);
    }

    public Technician update(Long id, Technician updated) {
        Technician existing = get(id);
        validateAvailability(updated.getAvailability());
        existing.setName(updated.getName());
        existing.setEmail(updated.getEmail());
        existing.setPhone(updated.getPhone());
        existing.setSpecialty(updated.getSpecialty());
        existing.setSkills(updated.getSkills());
        if (updated.getAvailability() != null && !updated.getAvailability().isBlank()) {
            existing.setAvailability(updated.getAvailability().toUpperCase());
        }
        return technicians.save(existing);
    }

    public void deactivate(Long id) {
        Technician technician = get(id);
        technician.setActive(false);
        technician.setAvailability("UNAVAILABLE");
        technicians.save(technician);
    }

    private void validateAvailability(String availability) {
        if (availability != null && !availability.isBlank()
                && !AVAILABILITIES.contains(availability.trim().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Availability must be AVAILABLE, BUSY, or UNAVAILABLE");
        }
    }
}
