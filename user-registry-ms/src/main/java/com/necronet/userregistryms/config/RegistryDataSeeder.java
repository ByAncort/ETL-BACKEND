package com.necronet.userregistryms.config;

import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserRole;
import com.necronet.userregistryms.entity.UserStatus;
import com.necronet.userregistryms.repository.UserRepository;
import com.necronet.userregistryms.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistryDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        String adminUsername = "etladmin";
        
        if (!userRepository.existsByUsername(adminUsername)) {
            boolean hasOtherAdmins = userRoleRepository.existsByRolId(1L);
            UserStatus finalStatus = hasOtherAdmins ? UserStatus.pending_verification : UserStatus.active;
            
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail("etladmin@etladmin.com");
            admin.setPasswordHash(passwordEncoder.encode("etladmin"));
            admin.setFirstName("Admin");
            admin.setLastName("System");
            admin.setStatus(finalStatus);
            admin.setEmailVerifiedAt(hasOtherAdmins ? null : LocalDateTime.now());
            
            User savedAdmin = userRepository.save(admin);
            
            UserRole userRole = new UserRole();
            userRole.setUsuarioId(savedAdmin.getId());
            userRole.setRolId(1L); // 1L = ROLE_ADMIN
            userRole.setAssignedBy(savedAdmin.getId());
            userRoleRepository.save(userRole);
            
            log.info("Admin user 'etladmin' seeded in user-registry-ms (Status: {}).", finalStatus);
        } else {
            log.info("Admin user 'etladmin' already exists in user-registry-ms.");
        }
    }
}
