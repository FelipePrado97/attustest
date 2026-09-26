package br.com.attus.processos.adapter.out.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({OutboxLimpeza.class, OutboxPersistenciaTest.Config.class})
class OutboxPersistenciaTest {

    private static final Instant AGORA = Instant.parse("2025-06-10T15:00:00Z");

    @TestConfiguration
    static class Config {
        @Bean
        Clock clock() {
            return Clock.fixed(AGORA, ZoneOffset.UTC);
        }

        @Bean
        OutboxProperties outboxProperties() {
            return new OutboxProperties(Duration.ofSeconds(1), Duration.ofMinutes(5), Duration.ofDays(7));
        }
    }

    @Autowired
    private OutboxEventoRepository repository;
    @Autowired
    private OutboxLimpeza limpeza;

    @BeforeEach
    void limpar() {
        repository.deleteAll();
    }

    @Test
    void pendentesVemNaOrdemDeInsercaoMesmoComCriadoEmEmpatado() {
        var primeiro = salvar(AGORA);
        var segundo = salvar(AGORA);
        var terceiro = salvar(AGORA);

        assertThat(repository.findTop500ByPublicadoEmIsNullOrderBySequenciaAsc())
                .extracting(OutboxEventoEntity::getId)
                .containsExactly(primeiro.getId(), segundo.getId(), terceiro.getId());
    }

    @Test
    void contadoresDasMetricas() {
        salvar(AGORA);
        var comFalha = salvar(AGORA);
        comFalha.registrarFalha("erro", null);
        var publicado = salvar(AGORA);
        publicado.marcarPublicado(AGORA);
        repository.saveAllAndFlush(List.of(comFalha, publicado));

        assertThat(repository.countByPublicadoEmIsNull()).isEqualTo(2);
        assertThat(repository.countByPublicadoEmIsNullAndTentativasGreaterThan(0)).isEqualTo(1);
    }

    @Test
    void limpezaRemoveSoPublicadosForaDaRetencao() {
        var antigo = publicadoEm(AGORA.minus(Duration.ofDays(8)));
        var recente = publicadoEm(AGORA.minus(Duration.ofDays(1)));
        var pendente = salvar(AGORA.minus(Duration.ofDays(30)));

        var removidos = limpeza.expurgarPublicados();

        assertThat(removidos).isEqualTo(1);
        assertThat(repository.findAll()).extracting(OutboxEventoEntity::getId)
                .containsExactlyInAnyOrder(recente.getId(), pendente.getId())
                .doesNotContain(antigo.getId());
    }

    private OutboxEventoEntity publicadoEm(Instant quando) {
        var evento = salvar(quando);
        evento.marcarPublicado(quando);
        return repository.saveAndFlush(evento);
    }

    private OutboxEventoEntity salvar(Instant criadoEm) {
        return repository.saveAndFlush(new OutboxEventoEntity(UUID.randomUUID(), UUID.randomUUID(), "CADASTRADO", "{}", null, criadoEm));
    }
}
