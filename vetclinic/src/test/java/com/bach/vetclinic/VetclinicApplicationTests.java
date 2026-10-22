package com.bach.vetclinic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class VetclinicApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void createsTheTwelveTablesFromTheErDiagramAndPrescriptionExtension() {
        Set<String> tableNames = Set.copyOf(jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'",
                String.class));

        assertTrue(tableNames.containsAll(Set.of(
                "USERS",
                "OWNERS",
                "PETS",
                "VETS",
                "VISITS",
                "VACCINATIONS",
                "PET_WEIGHT_RECORDS",
                "INVOICES",
                "INVOICE_ITEMS",
                "MEDICINES",
                "PRESCRIPTIONS",
                "PRESCRIPTION_ITEMS",
                "SUPPORT_REQUESTS",
                "KNOWLEDGE_DOCUMENTS",
                "VACCINE_TYPES",
                "REVOKED_TOKENS"
        )), "The database should contain the nine original tables and three approved pharmacy tables");
    }

    @Test
    void createsTheTenForeignKeyRelationshipsIncludingPrescriptions() {
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE TABLE_SCHEMA = 'PUBLIC' AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                Integer.class);

        assertTrue(foreignKeyCount != null && foreignKeyCount == 12,
                "The database should contain twelve foreign keys including vaccine catalog and user-vet relationships");
    }
}

