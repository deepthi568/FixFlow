package FixFlow_backend.service;

import FixFlow_backend.dto.CreateUserRequest;
import FixFlow_backend.dto.UpdateUserRequest;
import FixFlow_backend.dto.UserResponse;
import FixFlow_backend.model.AppUser;
import FixFlow_backend.model.Technician;
import FixFlow_backend.repository.AppUserRepository;
import FixFlow_backend.repository.TechnicianRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final AppUserRepository users;
    private final TechnicianRepository technicians;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users,
                       TechnicianRepository technicians,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.technicians = technicians;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return users.findAllWithTechnician().stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(find(id));
    }

    public UserResponse create(CreateUserRequest request) {
        if (users.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setRole(request.getRole());
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        user.setTechnician(resolveTechnician(request.getRole(), request.getTechnicianId()));
        return UserResponse.from(users.save(user));
    }

    public UserResponse update(Long id, UpdateUserRequest request) {
        AppUser user = find(id);
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getEnabled() != null) {
            if (!request.getEnabled() && user.getRole() == AppUser.Role.ADMIN
                    && users.countByRole(AppUser.Role.ADMIN) <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot disable the last administrator");
            }
            user.setEnabled(request.getEnabled());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        AppUser.Role role = request.getRole() != null ? request.getRole() : user.getRole();
        if (role == AppUser.Role.TECHNICIAN) {
            Long techId = request.getTechnicianId() != null
                    ? request.getTechnicianId()
                    : (user.getTechnician() != null ? user.getTechnician().getId() : null);
            user.setTechnician(resolveTechnician(role, techId));
        } else {
            user.setTechnician(null);
        }
        return UserResponse.from(users.save(user));
    }

    private AppUser find(Long id) {
        return users.findByIdWithTechnician(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Technician resolveTechnician(AppUser.Role role, Long technicianId) {
        if (role != AppUser.Role.TECHNICIAN) {
            return null;
        }
        if (technicianId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Technician accounts must be linked to a technician record");
        }
        Technician tech = technicians.findById(technicianId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Technician not found"));
        if (!tech.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot link an inactive technician");
        }
        return tech;
    }
}
