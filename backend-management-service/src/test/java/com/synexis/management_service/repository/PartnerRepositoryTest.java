package com.synexis.management_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.synexis.management_service.models.Partner;
import com.synexis.management_service.models.PartnerAvailabilityStatus;
import com.synexis.management_service.models.UserRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PartnerRepositoryTest {

    @Autowired
    private PartnerRepository partnerRepository;

    @Test
    void saveAndFindByEmail_persistsPartner() {
        Partner partner = new Partner();
        partner.setEmail("partner@example.com");
        partner.setPasswordHash("$2a$10$fakehashforrepositorytest");
        partner.setName("Tour Guides Inc");
        partner.setAreaId(1);
        partner.setAvailabilityStatus(PartnerAvailabilityStatus.available);
        partner.setTermsAccepted(true);
        partner.setRole(UserRole.partner);
        partner.setCreatedAt(Instant.now());

        Partner saved = partnerRepository.save(partner);

        assertThat(saved.getId()).isNotNull();
        assertThat(partnerRepository.findByEmailIgnoreCase("partner@example.com"))
                .isPresent()
                .get()
                .satisfies(
                        p -> {
                            assertThat(p.getEmail()).isEqualTo("partner@example.com");
                            assertThat(p.getName()).isEqualTo("Tour Guides Inc");
                            assertThat(p.getAreaId()).isEqualTo(1);
                            assertThat(p.getPasswordHash()).startsWith("$2a$");
                            assertThat(p.getRole()).isEqualTo(UserRole.partner);
                        });
    }
}
