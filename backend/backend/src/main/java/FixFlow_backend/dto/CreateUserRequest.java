package FixFlow_backend.dto;

import FixFlow_backend.model.AppUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    private String password;

    @Size(max = 128)
    private String fullName;

    @NotNull(message = "Role is required")
    private AppUser.Role role;

    private Long technicianId;

    private Boolean enabled = true;

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public AppUser.Role getRole() { return role; }
    public Long getTechnicianId() { return technicianId; }
    public Boolean getEnabled() { return enabled; }

    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(AppUser.Role role) { this.role = role; }
    public void setTechnicianId(Long technicianId) { this.technicianId = technicianId; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
