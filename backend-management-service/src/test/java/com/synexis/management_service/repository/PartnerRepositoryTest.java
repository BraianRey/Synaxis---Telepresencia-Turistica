package com.synexis.management_service.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.synexis.management_service.entity.Area;
import com.synexis.management_service.entity.Partner;
import com.synexis.management_service.entity.PartnerAvailabilityStatus;
import com.synexis.management_service.entity.UserRole;

@SpringBootTest
@Transactional
class PartnerRepositoryTest {

    @Autowired
    private PartnerRepository partnerRepository;
    @Autowired
    private AreaRepository areaRepository;

    @Test
    void saveAndFindByEmail_persistsPartner() {
        Area area = new Area();
        area.setCountry("Colombia");
        area.setState("Cauca");
        area.setMunicipality("Popayán");
        area.setCenterLat(2.4448);
        area.setCenterLng(-76.6147);
        Area savedArea = areaRepository.save(area);

        Partner partner = new Partner();
        partner.setEmail("partner@example.com");
        partner.setName("Tour Guides Inc");
        partner.setArea(savedArea);
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
                            assertThat(p.getArea().getId()).isEqualTo(savedArea.getId());
                            assertThat(p.getRole()).isEqualTo(UserRole.partner);
                        });
    }
}
