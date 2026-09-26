package br.com.attus.processos.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

interface ProcessoJpaRepository extends JpaRepository<ProcessoJpaEntity, UUID>, JpaSpecificationExecutor<ProcessoJpaEntity> {

    boolean existsByNumeroCnj(String numeroCnj);
}
