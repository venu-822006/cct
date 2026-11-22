package com.clouddrive.config;

import com.clouddrive.entity.User;
import com.clouddrive.repository.UserRepository;
import com.clouddrive.service.AuthService;
import com.clouddrive.dto.RegisterRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AuthService authService;

    public DataInitializer(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            RegisterRequest adminReq = new RegisterRequest();
            adminReq.setUsername("admin");
            adminReq.setEmail("admin@clouddrive.com");
            adminReq.setPassword("admin123");
            adminReq.setFullName("System Administrator");
            try {
                authService.register(adminReq);
                User admin = userRepository.findByUsername("admin").orElse(null);
                if (admin != null) {
                    admin.setRole("ROLE_ADMIN");
                    userRepository.save(admin);
                }
            } catch (Exception ignored) {}
        }
    }
}
