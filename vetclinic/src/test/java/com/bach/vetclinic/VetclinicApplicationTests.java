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
                "SELECT lower(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES WHERE lower(TABLE_SCHEMA) = 'public'",
                String.class));

        assertTrue(tableNames.containsAll(Set.of(
                "users",
                "owners",
                "pets",
                "vets",
                "visits",
                "vaccinations",
                "pet_weight_records",
                "invoices",
                "invoice_items",
                "medicines",
                "prescriptions",
                "prescription_items",
                "support_requests",
                "knowledge_documents",
                "vaccine_types",
                "revoked_tokens"
        )), "The database should contain the nine original tables and three approved pharmacy tables");
    }

    @Test
    void createsTheTenForeignKeyRelationshipsIncludingPrescriptions() {
        Integer foreignKeyCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                        "WHERE lower(TABLE_SCHEMA) = 'public' AND CONSTRAINT_TYPE = 'FOREIGN KEY'",
                Integer.class);

        assertTrue(foreignKeyCount != null && foreignKeyCount == 12,
                "The database should contain twelve foreign keys including vaccine catalog and user-vet relationships");
    }
}

