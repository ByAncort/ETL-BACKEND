package com.necronet.userregistryms;

import com.necronet.userregistryms.entity.Role;
import com.necronet.userregistryms.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Optional;

@SpringBootApplication
@Slf4j
public class UserRegistryMsApplication implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    public static void main(String[] args) {
        SpringApplication.run(UserRegistryMsApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        initializeRoles();
    }

    private void initializeRoles() {
        createRoleIfNotExists("ROLE_ADMIN", "Administrator role with full access", 1L, true);
        createRoleIfNotExists("ROLE_USER", "Standard user role", 2L, true);
        createRoleIfNotExists("ROLE_MODERATOR", "Moderator role with limited admin access", 3L, true);
        createRoleIfNotExists("ROLE_GUEST", "Guest role with minimal access", 4L, true);
        log.info("Default roles initialized");
    }

    private void createRoleIfNotExists(String name, String description, Long levelRole, Boolean isSystem) {
        Optional<Role> existingRole = roleRepository.findByName(name);
        if (existingRole.isEmpty()) {
            Role role = new Role();
            role.setName(name);
            role.setDescription(description);
            role.setLevelRole(levelRole);
            role.setSystem(isSystem);
            roleRepository.save(role);
            log.info("Created role: {}", name);
        }
    }
}
