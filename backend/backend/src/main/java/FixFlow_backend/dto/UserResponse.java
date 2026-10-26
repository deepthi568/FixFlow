package FixFlow_backend.dto;

import FixFlow_backend.model.AppUser;
import FixFlow_backend.security.AuthUserDetails;

import java.time.LocalDateTime;

public class UserResponse {

    private Long id;
    private String username;
    private String fullName;
    private AppUser.Role role;
    private Long technicianId;
    private boolean enabled;
    private LocalDateTime createdAt;

    public static UserResponse from(AppUser user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.fullName = user.getFullName();
        dto.role = user.getRole();
        dto.technicianId = user.getTechnician() != null ? user.getTechnician().getId() : null;
        dto.enabled = user.isEnabled();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public static UserResponse from(AuthUserDetails user) {
        UserResponse dto = new UserResponse();
        dto.id = user.getId();
        dto.username = user.getUsername();
        dto.fullName = user.getFullName();
        dto.role = user.getRole();
        dto.technicianId = user.getTechnicianId();
        dto.enabled = user.isEnabled();
        return dto;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public AppUser.Role getRole() { return role; }
    public Long getTechnicianId() { return technicianId; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
