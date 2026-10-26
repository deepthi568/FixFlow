package FixFlow_backend.service;

import FixFlow_backend.model.AppUser;
import FixFlow_backend.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator when FIXFLOW_ADMIN_USERNAME and FIXFLOW_ADMIN_PASSWORD
 * are set and that username does not already exist.
 *
 * <p>Credentials are never hardcoded. If they are omitted, the application starts
 * without seeding an admin (existing users are left unchanged).</p>
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    @Value("${fixflow.admin.username:}")
    private String adminUsername;

    @Value("${fixflow.admin.password:}")
    private String adminPassword;

    @Value("${fixflow.admin.full-name:Administrator}")
    private String adminFullName;

    public AdminBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminUsername == null || adminUsername.isBlank()) {
            log.info("No FIXFLOW_ADMIN_USERNAME set; skipping administrator bootstrap.");
            return;
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "FIXFLOW_ADMIN_USERNAME is set but FIXFLOW_ADMIN_PASSWORD is empty.");
        }
        if (adminPassword.length() < 8) {
            throw new IllegalStateException("FIXFLOW_ADMIN_PASSWORD must be at least 8 characters.");
        }
        if (users.existsByUsername(adminUsername.trim())) {
            log.info("Administrator username already exists; bootstrap skipped.");
            return;
        }
        AppUser admin = new AppUser();
        admin.setUsername(adminUsername.trim());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(AppUser.Role.ADMIN);
        admin.setFullName(adminFullName);
        admin.setEnabled(true);
        users.save(admin);
        log.info("Created initial administrator '{}'. Change this password after first login.",
                admin.getUsername());
    }
}
