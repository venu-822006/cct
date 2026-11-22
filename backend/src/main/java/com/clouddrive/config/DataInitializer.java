package com.clouddrive.config;

import com.clouddrive.entity.User;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.service.AuthService;
import com.clouddrive.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${app.admin.bootstrap-enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.admin.username:}")
    private String adminUsername;

    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.email:admin@localhost}")
    private String adminEmail;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap is disabled. No default accounts created.");
            return;
        }

        if (!StringUtils.hasText(adminUsername) || !StringUtils.hasText(adminPassword)) {
            log.warn("ADMIN_BOOTSTRAP_ENABLED is true, but ADMIN_BOOTSTRAP_USERNAME or ADMIN_BOOTSTRAP_PASSWORD is not set. Skipping admin creation.");
            return;
        }

        if (!userRepository.existsByUsername(adminUsername)) {
            RegisterRequest adminReq = new RegisterRequest();
            adminReq.setUsername(adminUsername);
            adminReq.setEmail(adminEmail);
            adminReq.setPassword(adminPassword);
            adminReq.setFullName("System Administrator");
            try {
                authService.register(adminReq);
                User admin = userRepository.findByUsername(adminUsername).orElse(null);
                if (admin != null) {
                    admin.setRole("ROLE_ADMIN");
                    userRepository.save(admin);
                    log.info("Successfully bootstrapped initial admin account: {}", adminUsername);
                }
            } catch (Exception e) {
                log.error("Failed to bootstrap administrator account", e);
            }
        }
    }
}
