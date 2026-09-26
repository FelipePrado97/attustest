import { HttpErrorResponse } from '@angular/common/http';

import { Processo, RegistroHistorico } from '../app/processos/processo.model';

export function processoExemplo(sobrescrever: Partial<Processo> = {}): Processo {
  return {
    id: 'p-1',
    numeroCnj: '0001234-71.2024.8.26.0100',
    assunto: 'Execução fiscal - IPTU 2021',
    parteContraria: 'Empresa Exemplo Ltda',
    valorCausa: 15230.5,
    dataDistribuicao: '2024-03-15',
    status: 'ATIVO',
    transicoesPermitidas: ['SUSPENSO', 'ARQUIVADO'],
    versao: 0,
    criadoEm: '2025-06-10T15:00:00Z',
    atualizadoEm: '2025-06-10T15:00:00Z',
    ...sobrescrever,
  };
}

export function eventoExemplo(sobrescrever: Partial<RegistroHistorico> = {}): RegistroHistorico {
  return {
    eventoId: 'e-1',
    tipo: 'CADASTRADO',
    descricao: 'Processo cadastrado',
    ocorridoEm: '2025-06-10T15:00:00.100Z',
    ...sobrescrever,
  };
}

export function erroHttp(status: number, corpo: Record<string, unknown> = {}): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: { status, correlationId: 'cid-teste', ...corpo } });
}
