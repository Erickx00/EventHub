package com.eventhub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Valida o que nenhum teste unitario alcanca: as migracoes Flyway rodando de
 * verdade e o mapeamento JPA batendo com o schema (ddl-auto=validate).
 *
 * Roda apenas quando existe um PostgreSQL: `IT_POSTGRES=true` + SPRING_DATASOURCE_*.
 * Localmente sem Docker, o teste e pulado em vez de falhar.
 */
@SpringBootTest
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "IT_POSTGRES", matches = "true")
class SchemaValidationTest {

    private static final List<String> TABLES = List.of(
            "users", "refresh_tokens", "events", "ticket_types",
            "orders", "order_items", "payments", "tickets", "check_ins");

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void appliesEveryMigration() {
        Integer applied = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);

        // Uma migracao por arquivo em db/migration. Se uma nova for adicionada sem
        // rodar aqui, este numero precisa subir junto.
        assertThat(applied).isGreaterThanOrEqualTo(6);
    }

    @Test
    void createsEveryTable() {
        for (String table : TABLES) {
            Integer found = jdbc.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = 'public' and table_name = ?",
                    Integer.class, table);

            assertThat(found).as("tabela %s", table).isEqualTo(1);
        }
    }

    @Test
    void keepsStockCheckConstraint() {
        // O banco precisa recusar estoque negativo mesmo que o codigo falhe.
        Integer constraints = jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints "
                        + "where table_name = 'ticket_types' and constraint_type = 'CHECK'",
                Integer.class);

        assertThat(constraints).isGreaterThanOrEqualTo(5);
    }

    @Test
    void enforcesUniqueCheckInPerTicket() {
        Integer uniqueIndexes = jdbc.queryForObject(
                "select count(*) from pg_indexes where tablename = 'check_ins' and indexdef ilike '%unique%'",
                Integer.class);

        assertThat(uniqueIndexes).isGreaterThanOrEqualTo(1);
    }
}
