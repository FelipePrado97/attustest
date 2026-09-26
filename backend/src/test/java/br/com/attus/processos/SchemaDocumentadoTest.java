package br.com.attus.processos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class SchemaDocumentadoTest {

    private static final String TABELAS_DA_APLICACAO = """
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_type = 'BASE TABLE'
              AND table_name <> 'flyway_schema_history'
            """;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void todasAsTabelasTemComentario() {
        var tabelas = jdbc.queryForList("SELECT table_name " + TABELAS_DA_APLICACAO, String.class);
        var semComentario = jdbc.queryForList(
                "SELECT table_name " + TABELAS_DA_APLICACAO + " AND (remarks IS NULL OR remarks = '')", String.class);

        assertThat(tabelas).containsExactlyInAnyOrder("processo", "outbox_evento", "historico_processo");
        assertThat(semComentario).as("tabelas sem COMMENT ON TABLE").isEmpty();
    }

    @Test
    void todasAsColunasTemComentario() {
        var semComentario = jdbc.queryForList("""
                SELECT table_name || '.' || column_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name <> 'flyway_schema_history'
                  AND (remarks IS NULL OR remarks = '')
                """, String.class);

        assertThat(semComentario).as("colunas sem COMMENT ON COLUMN").isEmpty();
    }
}
