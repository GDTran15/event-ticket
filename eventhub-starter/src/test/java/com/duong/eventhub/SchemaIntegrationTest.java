package com.duong.eventhub;

import static org.assertj.core.api.Assertions.assertThat;
import com.duong.eventhub.user.AppUser;
import com.duong.eventhub.user.AppUserRepository;
import com.duong.eventhub.user.Role;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(properties = "app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Testcontainers
class SchemaIntegrationTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    @Autowired AppUserRepository users;
    @Autowired EntityManager entityManager;

    @Test
    @Transactional
    void flywaySchemaMatchesJpaAndPersistsUserUpdatesAndRoles() {
        AppUser user = new AppUser();
        user.setEmail("customer@example.com"); user.setPasswordHash("bcrypt-hash-placeholder");
        user.setDisplayName("Customer"); user.setEnabled(true);
        user.getRoles().add(Role.CUSTOMER);
        UUID userId = user.getId();
        assertThat(userId).isNotNull();

        users.saveAndFlush(user);
        entityManager.clear();

        AppUser persisted = users.findByEmail("customer@example.com").orElseThrow();
        assertThat(persisted.getId()).isEqualTo(userId);
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getRoles()).containsExactly(Role.CUSTOMER);

        persisted.setDisplayName("Updated Customer");
        persisted.getRoles().add(Role.STAFF);
        users.saveAndFlush(persisted);
        entityManager.clear();

        AppUser updated = users.findById(userId).orElseThrow();
        assertThat(updated.getDisplayName()).isEqualTo("Updated Customer");
        assertThat(updated.getRoles()).containsExactlyInAnyOrder(Role.CUSTOMER, Role.STAFF);
    }
}
