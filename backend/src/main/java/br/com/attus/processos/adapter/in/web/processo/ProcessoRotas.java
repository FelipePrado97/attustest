package br.com.attus.processos.adapter.in.web.processo;

final class ProcessoRotas {

    static final String BASE = "/api/v1/processos";
    static final String POR_ID = BASE + "/{id}";
    static final String STATUS = POR_ID + "/status";
    static final String HISTORICO = POR_ID + "/historico";

    static final String TAG = "Processos";

    private ProcessoRotas() {
    }
}
