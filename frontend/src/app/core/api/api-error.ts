import { HttpErrorResponse } from '@angular/common/http';

export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export class ApiError {
  constructor(
    readonly status: number,
    readonly mensagem: string,
    readonly erros: readonly ErroCampo[] = [],
    readonly correlationId?: string,
  ) {}

  get semConexao(): boolean {
    return this.status === 0;
  }

  get conflito(): boolean {
    return this.status === 409;
  }

  get naoEncontrado(): boolean {
    return this.status === 404;
  }

  static de(erro: unknown): ApiError {
    if (erro instanceof ApiError) {
      return erro;
    }
    if (!(erro instanceof HttpErrorResponse)) {
      return new ApiError(-1, 'Erro inesperado na aplicação.');
    }
    if (erro.status === 0) {
      return new ApiError(0, 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.');
    }
    const corpo = ehObjeto(erro.error) ? erro.error : {};
    const correlationId =
      texto(corpo['correlationId']) ?? erro.headers?.get('X-Correlation-Id') ?? undefined;
    return new ApiError(
      erro.status,
      texto(corpo['detail']) ?? mensagemPadrao(erro.status),
      errosDeCampo(corpo['erros']),
      correlationId,
    );
  }
}

function mensagemPadrao(status: number): string {
  if (status >= 500) {
    return 'Erro interno no servidor. Tente novamente em instantes.';
  }
  return 'Não foi possível concluir a operação.';
}

function errosDeCampo(valor: unknown): ErroCampo[] {
  if (!Array.isArray(valor)) {
    return [];
  }
  return valor
    .filter(ehObjeto)
    .map((e) => ({ campo: texto(e['campo']) ?? '', mensagem: texto(e['mensagem']) ?? '' }))
    .filter((e) => e.campo !== '' && e.mensagem !== '');
}

function ehObjeto(valor: unknown): valor is Record<string, unknown> {
  return typeof valor === 'object' && valor !== null;
}

function texto(valor: unknown): string | undefined {
  return typeof valor === 'string' && valor.trim() !== '' ? valor : undefined;
}
