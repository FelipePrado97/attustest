import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { ProcessoApi } from './processo-api';
import { DadosProcesso } from './processo.model';

describe('ProcessoApi', () => {
  const BASE = '/api/v1/processos';
  const dados: DadosProcesso = {
    assunto: 'Execução fiscal',
    parteContraria: 'Contribuinte',
    valorCausa: 1500,
    dataDistribuicao: '2024-03-15',
  };

  let api: ProcessoApi;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ProcessoApi);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('listar envia só os filtros preenchidos', () => {
    api.listar({ termo: '  iptu  ', status: 'SUSPENSO', pagina: 2, tamanho: 20 }).subscribe();

    const req = backend.expectOne((r) => r.url === BASE);
    expect(req.request.params.get('termo')).toBe('iptu');
    expect(req.request.params.get('status')).toBe('SUSPENSO');
    expect(req.request.params.get('pagina')).toBe('2');
    expect(req.request.params.get('tamanho')).toBe('20');
    req.flush({ itens: [], pagina: 2, tamanho: 20, totalItens: 0, totalPaginas: 0 });
  });

  it('listar omite termo em branco e status vazio', () => {
    api.listar({ termo: '   ', pagina: 0, tamanho: 10 }).subscribe();

    const req = backend.expectOne((r) => r.url === BASE);
    expect(req.request.params.has('termo')).toBe(false);
    expect(req.request.params.has('status')).toBe(false);
    req.flush({ itens: [], pagina: 0, tamanho: 10, totalItens: 0, totalPaginas: 0 });
  });

  it('cadastrar faz POST com o número CNJ', () => {
    api.cadastrar({ ...dados, numeroCnj: '0001234-71.2024.8.26.0100' }).subscribe();

    const req = backend.expectOne(BASE);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.numeroCnj).toBe('0001234-71.2024.8.26.0100');
    req.flush({});
  });

  it('atualizar faz PUT enviando a versão lida', () => {
    api.atualizar('abc', 3, dados).subscribe();

    const req = backend.expectOne(`${BASE}/abc`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ ...dados, versao: 3 });
    req.flush({});
  });

  it('alterarStatus faz PATCH em /status', () => {
    api.alterarStatus('abc', 1, 'ARQUIVADO').subscribe();

    const req = backend.expectOne(`${BASE}/abc/status`);
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ status: 'ARQUIVADO', versao: 1 });
    req.flush({});
  });

  it('buscar, excluir e historico usam as rotas do recurso', () => {
    api.buscar('abc').subscribe();
    api.excluir('abc').subscribe();
    api.historico('abc').subscribe();

    const recurso = backend.match(`${BASE}/abc`);
    expect(recurso.map((r) => r.request.method)).toEqual(['GET', 'DELETE']);
    recurso.forEach((r) => r.flush(null));
    backend.expectOne(`${BASE}/abc/historico`).flush([]);
  });
});
