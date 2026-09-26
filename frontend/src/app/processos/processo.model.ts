export type StatusProcesso = 'ATIVO' | 'SUSPENSO' | 'ARQUIVADO';

export type TipoEvento = 'CADASTRADO' | 'ATUALIZADO' | 'STATUS_ALTERADO' | 'EXCLUIDO';

export const TODOS_STATUS: readonly StatusProcesso[] = ['ATIVO', 'SUSPENSO', 'ARQUIVADO'];

export interface Processo {
  id: string;
  numeroCnj: string;
  assunto: string;
  parteContraria: string;
  valorCausa: number;
  dataDistribuicao: string;
  status: StatusProcesso;
  transicoesPermitidas: StatusProcesso[];
  versao: number;
  criadoEm: string;
  atualizadoEm: string;
}

export interface DadosProcesso {
  assunto: string;
  parteContraria: string;
  valorCausa: number;
  dataDistribuicao: string;
}

export interface NovoProcesso extends DadosProcesso {
  numeroCnj: string;
}

export interface RegistroHistorico {
  eventoId: string;
  tipo: TipoEvento;
  descricao: string;
  ocorridoEm: string;
}

export interface Pagina<T> {
  itens: T[];
  pagina: number;
  tamanho: number;
  totalItens: number;
  totalPaginas: number;
}

export interface FiltroProcessos {
  termo?: string;
  status?: StatusProcesso;
  pagina: number;
  tamanho: number;
}

export const LIMITES = {
  assunto: 200,
  parteContraria: 150,
  valorCausaMaximo: 9_999_999_999_999.99,
} as const;
