package FixFlow_backend.dto;

import FixFlow_backend.model.AppUser;
import jakarta.validation.constraints.Size;

public class UpdateUserRequest {

    @Size(max = 128)
    private String fullName;

    private AppUser.Role role;

    private Long technicianId;

    private Boolean enabled;

    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    public String getFullName() { return fullName; }
    public AppUser.Role getRole() { return role; }
    public Long getTechnicianId() { return technicianId; }
    public Boolean getEnabled() { return enabled; }
    public String getPassword() { return password; }

    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(AppUser.Role role) { this.role = role; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public void setPassword(String password) { this.password = password; }
}
