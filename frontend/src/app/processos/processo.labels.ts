import { StatusProcesso, TipoEvento } from './processo.model';

export const ROTULO_STATUS: Record<StatusProcesso, string> = {
  ATIVO: 'Ativo',
  SUSPENSO: 'Suspenso',
  ARQUIVADO: 'Arquivado',
};

export const ROTULO_EVENTO: Record<TipoEvento, string> = {
  CADASTRADO: 'Cadastro',
  ATUALIZADO: 'Atualização de dados',
  STATUS_ALTERADO: 'Mudança de status',
  EXCLUIDO: 'Exclusão',
};

export const ICONE_EVENTO: Record<TipoEvento, string> = {
  CADASTRADO: 'add_circle',
  ATUALIZADO: 'edit',
  STATUS_ALTERADO: 'swap_horiz',
  EXCLUIDO: 'delete',
};

export function rotuloAcaoStatus(origem: StatusProcesso, destino: StatusProcesso): string {
  switch (destino) {
    case 'SUSPENSO':
      return 'Suspender';
    case 'ARQUIVADO':
      return 'Arquivar';
    case 'ATIVO':
      return origem === 'ARQUIVADO' ? 'Desarquivar' : 'Reativar';
  }
}
