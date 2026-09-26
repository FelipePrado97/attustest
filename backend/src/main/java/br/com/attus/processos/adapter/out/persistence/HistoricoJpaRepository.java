package br.com.attus.processos.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HistoricoJpaRepository extends JpaRepository<HistoricoJpaEntity, UUID> {

    List<HistoricoJpaEntity> findByProcessoIdOrderBySequenciaDesc(UUID processoId);
}
