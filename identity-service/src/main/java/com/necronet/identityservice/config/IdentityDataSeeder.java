package com.necronet.identityservice.config;

import com.necronet.identityservice.entity.UserCredential;
import com.necronet.identityservice.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdentityDataSeeder implements CommandLineRunner {

    private final UserCredentialRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "etladmin";
        if (repository.findByUsername(adminUsername).isEmpty()) {
            boolean hasOtherAdmins = repository.existsByRole("ROLE_ADMIN");
            
            UserCredential admin = new UserCredential();
            admin.setUsername(adminUsername);
            admin.setEmail("etladmin@etladmin.com");
            admin.setPassword(passwordEncoder.encode("etladmin"));
            admin.setRole("ROLE_ADMIN");
            admin.setEnabled(!hasOtherAdmins); // Enable only if it's the first admin
            
            repository.save(admin);
            log.info("Admin user 'etladmin' seeded in identity-service (Enabled: {}).", !hasOtherAdmins);
        } else {
            log.info("Admin user 'etladmin' already exists in identity-service.");
        }
    }
}
