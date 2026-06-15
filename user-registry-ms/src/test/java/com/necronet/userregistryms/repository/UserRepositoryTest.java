package com.necronet.userregistryms.repository;

import com.necronet.userregistryms.entity.User;
import com.necronet.userregistryms.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    private User persistedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("johndoe");
        user.setEmail("john@example.com");
        user.setPasswordHash("$2a$10$hash");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setStatus(UserStatus.active);
        persistedUser = em.persistAndFlush(user);
        em.clear();
    }

    @Test
    void findUserByUsername_shouldReturnUser() {
        User found = userRepository.findUserByUsername("johndoe");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(persistedUser.getId());
        assertThat(found.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void findUserByUsername_shouldReturnNull_whenNotFound() {
        User found = userRepository.findUserByUsername("unknown");

        assertThat(found).isNull();
    }

    @Test
    void findByUsernameIgnoreCase_shouldReturnUser() {
        Optional<User> found = userRepository.findByUsernameIgnoreCase("JOHNDOE");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(persistedUser.getId());
    }

    @Test
    void findByUsernameIgnoreCase_shouldReturnEmpty_whenNotFound() {
        Optional<User> found = userRepository.findByUsernameIgnoreCase("nobody");

        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_shouldReturnUser() {
        Optional<User> found = userRepository.findByEmail("john@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("johndoe");
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenNotFound() {
        Optional<User> found = userRepository.findByEmail("unknown@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    void existsByUsername_shouldReturnTrue_whenExists() {
        boolean exists = userRepository.existsByUsername("johndoe");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_shouldReturnFalse_whenNotExists() {
        boolean exists = userRepository.existsByUsername("nobody");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_shouldReturnTrue_whenExists() {
        boolean exists = userRepository.existsByEmail("john@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_shouldReturnFalse_whenNotExists() {
        boolean exists = userRepository.existsByEmail("unknown@example.com");

        assertThat(exists).isFalse();
    }

    @Test
    void deleteById_shouldRemoveUser() {
        userRepository.deleteById(persistedUser.getId());
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findById(persistedUser.getId());
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByUsername_shouldRemoveUser() {
        userRepository.deleteByUsername("johndoe");
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findByEmail("john@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByEmail_shouldRemoveUser() {
        userRepository.deleteByEmail("john@example.com");
        em.flush();
        em.clear();

        Optional<User> found = userRepository.findByUsernameIgnoreCase("johndoe");
        assertThat(found).isEmpty();
    }
}
