package com.synexis.management_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.synexis.management_service.entity.Client;
import com.synexis.management_service.entity.UserRole;

@SpringBootTest
@Transactional
class ClientRepositoryTest {

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void saveAndFindByEmail_persistsClient() {
        Client client = new Client();
        client.setEmail("client@example.com");
        client.setName("Test User");
        client.setTermsAccepted(true);
        client.setRole(UserRole.CLIENT);
        client.setCreatedAt(Instant.now());

        Client saved = clientRepository.save(client);

        assertThat(saved.getId()).isNotNull();
        assertThat(clientRepository.findByEmailIgnoreCase("client@example.com"))
                .isPresent()
                .get()
                .satisfies(
                        c -> {
                            assertThat(c.getEmail()).isEqualTo("client@example.com");
                            assertThat(c.getName()).isEqualTo("Test User");
                            assertThat(c.getRole()).isEqualTo(UserRole.CLIENT);
                        });
    }
}
