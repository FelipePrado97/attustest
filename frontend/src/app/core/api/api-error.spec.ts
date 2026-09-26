import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';

import { ApiError } from './api-error';

describe('ApiError.de', () => {
  it('lê o ProblemDetail da API, incluindo erros de campo e correlationId', () => {
    const erro = ApiError.de(
      new HttpErrorResponse({
        status: 400,
        error: {
          detail: 'Um ou mais campos são inválidos',
          correlationId: 'cid-123',
          erros: [
            { campo: 'assunto', mensagem: 'Campo obrigatório' },
            { campo: '', mensagem: 'ignorado' },
            'formato inesperado',
          ],
        },
      }),
    );

    expect(erro.status).toBe(400);
    expect(erro.mensagem).toBe('Um ou mais campos são inválidos');
    expect(erro.correlationId).toBe('cid-123');
    expect(erro.erros).toEqual([{ campo: 'assunto', mensagem: 'Campo obrigatório' }]);
  });

  it('usa o header de correlação quando o corpo não traz o id', () => {
    const erro = ApiError.de(
      new HttpErrorResponse({ status: 502, error: 'Bad Gateway', headers: new HttpHeaders({ 'X-Correlation-Id': 'cid-h' }) }),
    );

    expect(erro.correlationId).toBe('cid-h');
    expect(erro.mensagem).toBe('Erro interno no servidor. Tente novamente em instantes.');
  });

  it('status 0 significa servidor inacessível', () => {
    const erro = ApiError.de(new HttpErrorResponse({ status: 0 }));

    expect(erro.semConexao).toBe(true);
    expect(erro.mensagem).toContain('Não foi possível conectar');
  });

  it('mensagem genérica para erro 4xx sem corpo', () => {
    expect(ApiError.de(new HttpErrorResponse({ status: 422 })).mensagem).toBe('Não foi possível concluir a operação.');
  });

  it('identifica conflito e não encontrado', () => {
    expect(ApiError.de(new HttpErrorResponse({ status: 409 })).conflito).toBe(true);
    expect(ApiError.de(new HttpErrorResponse({ status: 404 })).naoEncontrado).toBe(true);
  });

  it('erros que não vieram do HTTP viram erro inesperado', () => {
    expect(ApiError.de(new Error('bug')).mensagem).toBe('Erro inesperado na aplicação.');
  });

  it('não reembrulha um ApiError', () => {
    const original = new ApiError(409, 'Conflito');
    expect(ApiError.de(original)).toBe(original);
  });
});
