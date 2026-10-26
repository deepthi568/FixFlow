package FixFlow_backend.controller;

import FixFlow_backend.model.Technician;
import FixFlow_backend.service.TechnicianService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technicians")
public class TechnicianController {

    private final TechnicianService service;

    public TechnicianController(TechnicianService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public List<Technician> getAll() {
        return service.list(false);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Technician> getAllIncludingInactive() { return service.list(true); }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public Technician getById(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public Technician create(@Valid @RequestBody Technician technician) {
        return service.create(technician);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Technician update(@PathVariable Long id, @Valid @RequestBody Technician updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivate(@PathVariable Long id) {
        service.deactivate(id);
    }
}
