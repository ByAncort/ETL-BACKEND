package com.necronet.identityservice.repository;

import com.necronet.identityservice.entity.UserCredential;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserCredentialRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserCredentialRepository repository;

    @Test
    void findByUsername_whenExists_shouldReturnUser() {
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        user.setEnabled(true);
        em.persist(user);
        em.flush();
        em.clear();

        Optional<UserCredential> result = repository.findByUsername("testuser");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByUsername_whenNotExists_shouldReturnEmpty() {
        em.flush();
        em.clear();

        Optional<UserCredential> result = repository.findByUsername("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_whenExists_shouldReturnUser() {
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        user.setEnabled(true);
        em.persist(user);
        em.flush();
        em.clear();

        Optional<UserCredential> result = repository.findByEmail("test@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("testuser");
        assertThat(result.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void findByEmail_whenNotExists_shouldReturnEmpty() {
        em.flush();
        em.clear();

        Optional<UserCredential> result = repository.findByEmail("nonexistent@example.com");

        assertThat(result).isEmpty();
    }

    @Test
    void existsByUsername_whenExists_shouldReturnTrue() {
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        user.setEnabled(true);
        em.persist(user);
        em.flush();
        em.clear();

        boolean exists = repository.existsByUsername("testuser");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsername_whenNotExists_shouldReturnFalse() {
        em.flush();
        em.clear();

        boolean exists = repository.existsByUsername("nonexistent");

        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmail_whenExists_shouldReturnTrue() {
        UserCredential user = new UserCredential();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setRole("USER");
        user.setEnabled(true);
        em.persist(user);
        em.flush();
        em.clear();

        boolean exists = repository.existsByEmail("test@example.com");

        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_whenNotExists_shouldReturnFalse() {
        em.flush();
        em.clear();

        boolean exists = repository.existsByEmail("nonexistent@example.com");

        assertThat(exists).isFalse();
    }
}
