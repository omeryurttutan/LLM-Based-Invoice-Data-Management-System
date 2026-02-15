package com.faturaocr.infrastructure.persistence.user;

import com.faturaocr.BaseIntegrationTest;
import com.faturaocr.domain.company.entity.Company;
import com.faturaocr.domain.user.entity.User;
import com.faturaocr.domain.user.valueobject.Role;
import com.faturaocr.testutil.TestDataSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private TestDataSeeder testDataSeeder;

    private Company company;

    @BeforeEach
    void setUp() {
        company = testDataSeeder.seedCompany("User Repo Company", "8888888888");
    }

    @Test
    void findByEmail_ShouldReturnUser() {
        testDataSeeder.seedUser(company.getId(), "findme@repo.com", "Pass123!", Role.MANAGER);

        Optional<UserJpaEntity> found = userJpaRepository.findByEmail("findme@repo.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("findme@repo.com");
    }

    @Test
    void findByEmailAndCompanyId_ShouldReturnUser() {
        testDataSeeder.seedUser(company.getId(), "companyuser@repo.com", "Pass123!", Role.ACCOUNTANT);

        Optional<UserJpaEntity> found = userJpaRepository.findByEmailAndCompanyId("companyuser@repo.com",
                company.getId());

        assertThat(found).isPresent();
    }

    @Test
    void countByCompanyIdAndRole_ShouldReturnCorrectCount() {
        testDataSeeder.seedUser(company.getId(), "acc1@repo.com", "Pass123!", Role.ACCOUNTANT);
        testDataSeeder.seedUser(company.getId(), "acc2@repo.com", "Pass123!", Role.ACCOUNTANT);
        testDataSeeder.seedUser(company.getId(), "mgr1@repo.com", "Pass123!", Role.MANAGER);

        long count = userJpaRepository.countByCompanyIdAndRole(company.getId(), UserJpaEntity.RoleJpa.ACCOUNTANT);

        assertThat(count).isEqualTo(2);
    }
}
