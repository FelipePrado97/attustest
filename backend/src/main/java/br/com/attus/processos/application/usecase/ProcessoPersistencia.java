package br.com.attus.processos.application.usecase;

import br.com.attus.processos.application.port.out.ProcessoRepositoryPort;
import br.com.attus.processos.application.port.out.PublicadorEventosPort;
import br.com.attus.processos.domain.model.Processo;

public class ProcessoPersistencia {

    private final ProcessoRepositoryPort repositorio;
    private final PublicadorEventosPort publicador;

    public ProcessoPersistencia(ProcessoRepositoryPort repositorio, PublicadorEventosPort publicador) {
        this.repositorio = repositorio;
        this.publicador = publicador;
    }

    public Processo salvarEPublicar(Processo processo) {
        var eventos = processo.extrairEventos();
        var salvo = repositorio.salvar(processo);
        publicador.publicar(eventos);
        return salvo;
    }

    public void excluirEPublicar(Processo processo) {
        var eventos = processo.extrairEventos();
        repositorio.excluir(processo.getId());
        publicador.publicar(eventos);
    }
}
