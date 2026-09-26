package br.com.attus.processos.adapter.out.messaging;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface OutboxEventoRepository extends JpaRepository<OutboxEventoEntity, UUID> {

    List<OutboxEventoEntity> findTop500ByPublicadoEmIsNullOrderBySequenciaAsc();

    long countByPublicadoEmIsNull();

    long countByPublicadoEmIsNullAndTentativasGreaterThan(int tentativas);

    @Modifying
    @Query("delete from OutboxEventoEntity e where e.publicadoEm < :limite")
    int excluirPublicadosAntesDe(@Param("limite") Instant limite);
}
