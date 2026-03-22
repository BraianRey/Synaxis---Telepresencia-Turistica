package com.synexis.management_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.synexis.management_service.models.Role;
import com.synexis.management_service.models.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test: verifies that {@link UserRepository} can persist and read {@link User} entities using the test
 * datasource (H2) defined in {@code src/test/resources/application.yaml}.
 *
 * <p>How it works: {@link SpringBootTest} boots the full application context; {@link Transactional} rolls back after
 * the test so the database stays clean.
 */
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndFindByEmail_persistsUser() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("$2a$10$fakehashforrepositorytest");
        user.setRole(Role.CLIENT);

        User saved = userRepository.save(user);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findByEmailIgnoreCase("test@example.com"))
                .isPresent()
                .get()
                .satisfies(
                        u -> {
                            assertThat(u.getEmail()).isEqualTo("test@example.com");
                            assertThat(u.getRole()).isEqualTo(Role.CLIENT);
                            assertThat(u.getPasswordHash()).startsWith("$2a$");
                        });
    }
}
