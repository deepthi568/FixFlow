package FixFlow_backend.security;

import FixFlow_backend.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Snapshot of an {@link AppUser} used as the Spring Security principal.
 * Technician id is copied so assignment checks do not trigger lazy loads.
 */
public class AuthUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String passwordHash;
    private final AppUser.Role role;
    private final String fullName;
    private final Long technicianId;
    private final boolean enabled;

    public AuthUserDetails(AppUser user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
        this.fullName = user.getFullName();
        this.technicianId = user.getTechnician() != null ? user.getTechnician().getId() : null;
        this.enabled = user.isEnabled()
                && (user.getRole() != AppUser.Role.TECHNICIAN
                || (user.getTechnician() != null && user.getTechnician().isActive()));
    }

    public Long getId() { return id; }
    public AppUser.Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public Long getTechnicianId() { return technicianId; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}
