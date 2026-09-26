import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { TestBed } from '@angular/core/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { MatSelectHarness } from '@angular/material/select/testing';
import { Router, convertToParamMap, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { erroHttp, processoExemplo } from '../../../testing/processo.fixture';
import { Pagina, Processo } from '../processo.model';
import { provideLocalizacao } from '../../core/localizacao';
import { ProcessoApi } from '../processo-api';
import { ProcessoLista, filtroDaUrl } from './processo-lista';

describe('filtroDaUrl', () => {
  it('lê termo, status e paginação', () => {
    expect(filtroDaUrl(convertToParamMap({ termo: 'iptu', status: 'SUSPENSO', pagina: '2', tamanho: '20' }))).toEqual({
      termo: 'iptu',
      status: 'SUSPENSO',
      pagina: 2,
      tamanho: 20,
    });
  });

  it('usa os padrões quando não há parâmetros', () => {
    expect(filtroDaUrl(convertToParamMap({}))).toEqual({ termo: undefined, status: undefined, pagina: 0, tamanho: 10 });
  });

  it('descarta valores inválidos (URL editada à mão)', () => {
    expect(filtroDaUrl(convertToParamMap({ status: 'ENCERRADO', pagina: '-1', tamanho: '7' }))).toEqual({
      termo: undefined,
      status: undefined,
      pagina: 0,
      tamanho: 10,
    });
    expect(filtroDaUrl(convertToParamMap({ pagina: 'abc' })).pagina).toBe(0);
  });
});

describe('ProcessoLista', () => {
  const api = { listar: vi.fn() };
  let harness: RouterTestingHarness;
  let router: Router;

  function pagina(itens: Processo[], totalItens = itens.length): Pagina<Processo> {
    return { itens, pagina: 0, tamanho: 10, totalItens, totalPaginas: Math.ceil(totalItens / 10) };
  }

  beforeEach(async () => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'processos', component: ProcessoLista }]),
        provideLocalizacao(),
        { provide: ProcessoApi, useValue: api },
      ],
    });
    harness = await RouterTestingHarness.create();
    router = TestBed.inject(Router);
  });

  const texto = () => (harness.routeNativeElement as HTMLElement).textContent ?? '';

  it('carrega a lista aplicando os filtros da URL', async () => {
    api.listar.mockReturnValue(of(pagina([processoExemplo()])));

    await harness.navigateByUrl('/processos?termo=iptu&status=ATIVO&pagina=1');

    expect(api.listar).toHaveBeenCalledWith({ termo: 'iptu', status: 'ATIVO', pagina: 1, tamanho: 10 });
    expect(texto()).toContain('0001234-71.2024.8.26.0100');
    expect(texto()).toMatch(/R\$\s15\.230,50/);
    expect(texto()).toContain('Ativo');
    const busca = (harness.routeNativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    expect(busca.value).toBe('iptu');
  });

  it('sem cadastros mostra convite para cadastrar o primeiro', async () => {
    api.listar.mockReturnValue(of(pagina([])));

    await harness.navigateByUrl('/processos');

    expect(texto()).toContain('Nenhum processo cadastrado ainda.');
  });

  it('filtro sem resultado permite limpar os filtros', async () => {
    api.listar.mockReturnValue(of(pagina([])));
    await harness.navigateByUrl('/processos?termo=inexistente');
    expect(texto()).toContain('Nenhum processo encontrado para os filtros informados.');

    const limpar = Array.from((harness.routeNativeElement as HTMLElement).querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Limpar filtros'),
    ) as HTMLButtonElement;
    limpar.click();
    await harness.fixture.whenStable();

    expect(router.url).toBe('/processos');
  });

  it('falha ao carregar mostra o erro e permite tentar de novo', async () => {
    api.listar.mockReturnValueOnce(throwError(() => erroHttp(0))).mockReturnValueOnce(of(pagina([processoExemplo()])));
    await harness.navigateByUrl('/processos');
    expect(texto()).toContain('Não foi possível conectar ao servidor');

    const tentar = Array.from((harness.routeNativeElement as HTMLElement).querySelectorAll('button')).find((b) =>
      b.textContent?.includes('Tentar novamente'),
    ) as HTMLButtonElement;
    tentar.click();
    await harness.fixture.whenStable();

    expect(texto()).toContain('0001234-71.2024.8.26.0100');
  });

  it('digitar na busca atualiza a URL após uma pausa e volta para a primeira página', async () => {
    api.listar.mockReturnValue(of(pagina([processoExemplo()], 30)));
    await harness.navigateByUrl('/processos?pagina=2');

    const busca = (harness.routeNativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    busca.value = 'Execução';
    busca.dispatchEvent(new Event('input'));
    expect(router.url).toBe('/processos?pagina=2');

    await vi.waitFor(() => expect(decodeURIComponent(router.url)).toBe('/processos?termo=Execução'));
    expect(api.listar).toHaveBeenLastCalledWith({ termo: 'Execução', status: undefined, pagina: 0, tamanho: 10 });
  });

  it('paginação e tamanho de página vão para a URL', async () => {
    api.listar.mockReturnValue(of(pagina([processoExemplo()], 30)));
    await harness.navigateByUrl('/processos');
    const paginador = await TestbedHarnessEnvironment.loader(harness.fixture).getHarness(MatPaginatorHarness);

    await paginador.goToNextPage();
    expect(router.url).toBe('/processos?pagina=1');

    await paginador.setPageSize(20);
    expect(router.url).toBe('/processos?tamanho=20');
  });

  it('filtrar por status volta para a primeira página', async () => {
    api.listar.mockReturnValue(of(pagina([processoExemplo()], 30)));
    await harness.navigateByUrl('/processos?pagina=2');
    const status = await TestbedHarnessEnvironment.loader(harness.fixture).getHarness(MatSelectHarness);

    await status.open();
    await status.clickOptions({ text: 'Arquivado' });

    expect(router.url).toBe('/processos?status=ARQUIVADO');
  });
});
