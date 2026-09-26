import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { erroHttp, eventoExemplo, processoExemplo } from '../../../testing/processo.fixture';
import { FAKE_INTERVALO } from '../../../testing/tempo';
import { AcaoNotificacao, Notificacao } from '../../core/notificacao/notificacao';
import { provideLocalizacao } from '../../core/localizacao';
import { ProcessoApi } from '../processo-api';
import { ProcessoDetalhe, TENTATIVAS_HISTORICO, historicoPendente } from './processo-detalhe';

describe('historicoPendente', () => {
  const processo = processoExemplo({ atualizadoEm: '2025-06-10T15:00:00.500Z' });

  it('pendente quando nenhum evento é posterior à última alteração', () => {
    expect(historicoPendente(processo, [])).toBe(true);
    expect(historicoPendente(processo, [eventoExemplo({ ocorridoEm: '2025-06-10T15:00:00.400Z' })])).toBe(true);
  });

  it('em dia quando algum evento cobre a última alteração', () => {
    expect(historicoPendente(processo, [eventoExemplo({ ocorridoEm: '2025-06-10T15:00:00.500Z' })])).toBe(false);
    expect(historicoPendente(processo, [eventoExemplo({ ocorridoEm: '2025-06-10T15:00:01Z' })])).toBe(false);
  });
});

describe('ProcessoDetalhe', () => {
  let fixture: ComponentFixture<ProcessoDetalhe>;
  let elemento: HTMLElement;
  let router: Router;
  const api = {
    buscar: vi.fn(),
    historico: vi.fn(),
    alterarStatus: vi.fn(),
    excluir: vi.fn(),
  };
  const notificacao = { sucesso: vi.fn(), erro: vi.fn() };
  const dialog = { open: vi.fn() };

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      imports: [ProcessoDetalhe],
      providers: [
        provideRouter([]),
        provideLocalizacao(),
        { provide: ProcessoApi, useValue: api },
        { provide: Notificacao, useValue: notificacao },
        { provide: MatDialog, useValue: dialog },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => vi.useRealTimers());

  async function criar(): Promise<void> {
    fixture = TestBed.createComponent(ProcessoDetalhe);
    fixture.componentRef.setInput('id', 'p-1');
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  }

  function botao(texto: string): HTMLButtonElement {
    const encontrado = Array.from(elemento.querySelectorAll('button')).find((b) => b.textContent?.includes(texto));
    if (!encontrado) {
      throw new Error(`Botão "${texto}" não encontrado`);
    }
    return encontrado as HTMLButtonElement;
  }

  function usuarioResponde(confirmado: boolean): void {
    dialog.open.mockReturnValue({ afterClosed: () => of(confirmado) });
  }

  it('mostra os dados, a linha do tempo e só as transições permitidas', async () => {
    api.buscar.mockReturnValue(of(processoExemplo()));
    api.historico.mockReturnValue(of([eventoExemplo()]));

    await criar();

    expect(elemento.textContent).toContain('0001234-71.2024.8.26.0100');
    expect(elemento.textContent).toMatch(/R\$\s15\.230,50/);
    expect(elemento.textContent).toContain('15/03/2024');
    expect(elemento.textContent).toContain('Processo cadastrado');
    expect(botao('Suspender')).toBeTruthy();
    expect(botao('Arquivar')).toBeTruthy();
    expect(() => botao('Desarquivar')).toThrow();
  });

  it('processo arquivado não pode ser editado e oferece desarquivar', async () => {
    api.buscar.mockReturnValue(of(processoExemplo({ status: 'ARQUIVADO', transicoesPermitidas: ['ATIVO'] })));
    api.historico.mockReturnValue(of([eventoExemplo()]));

    await criar();

    expect(botao('Editar').disabled).toBe(true);
    expect(botao('Desarquivar')).toBeTruthy();
  });

  it('suspender envia a versão lida e atualiza a tela', async () => {
    api.buscar.mockReturnValue(of(processoExemplo({ versao: 2 })));
    api.historico.mockReturnValue(of([eventoExemplo()]));
    api.alterarStatus.mockReturnValue(
      of(processoExemplo({ status: 'SUSPENSO', versao: 3, transicoesPermitidas: ['ATIVO', 'ARQUIVADO'] })),
    );
    await criar();

    botao('Suspender').click();
    await fixture.whenStable();

    expect(api.alterarStatus).toHaveBeenCalledWith('p-1', 2, 'SUSPENSO');
    expect(dialog.open).not.toHaveBeenCalled();
    expect(notificacao.sucesso).toHaveBeenCalledWith('Status alterado para suspenso.');
    expect(botao('Reativar')).toBeTruthy();
  });

  it('arquivar exige confirmação', async () => {
    api.buscar.mockReturnValue(of(processoExemplo()));
    api.historico.mockReturnValue(of([eventoExemplo()]));
    api.alterarStatus.mockReturnValue(of(processoExemplo({ status: 'ARQUIVADO', transicoesPermitidas: ['ATIVO'] })));
    await criar();

    usuarioResponde(false);
    botao('Arquivar').click();
    await fixture.whenStable();
    expect(api.alterarStatus).not.toHaveBeenCalled();

    usuarioResponde(true);
    botao('Arquivar').click();
    await fixture.whenStable();
    expect(api.alterarStatus).toHaveBeenCalledWith('p-1', 0, 'ARQUIVADO');
  });

  it('conflito de versão ao mudar status oferece recarregar', async () => {
    api.buscar.mockReturnValue(of(processoExemplo()));
    api.historico.mockReturnValue(of([eventoExemplo()]));
    api.alterarStatus.mockReturnValue(throwError(() => erroHttp(409, { detail: 'Alterado por outro usuário' })));
    await criar();

    botao('Suspender').click();
    await fixture.whenStable();

    const acao = notificacao.erro.mock.calls[0][1] as AcaoNotificacao;
    expect(acao.rotulo).toBe('Recarregar');
    expect(botao('Suspender').disabled).toBe(false);
    acao.executar();
    expect(api.buscar).toHaveBeenCalledTimes(2);
  });

  it('erro comum ao mudar status é notificado sem ação extra', async () => {
    api.buscar.mockReturnValue(of(processoExemplo()));
    api.historico.mockReturnValue(of([eventoExemplo()]));
    api.alterarStatus.mockReturnValue(throwError(() => erroHttp(422, { detail: 'Transição não permitida' })));
    await criar();

    botao('Suspender').click();
    await fixture.whenStable();

    expect(notificacao.erro).toHaveBeenCalledTimes(1);
    expect(notificacao.erro.mock.calls[0][1]).toBeUndefined();
  });

  it('excluir pede confirmação e volta para a lista', async () => {
    api.buscar.mockReturnValue(of(processoExemplo()));
    api.historico.mockReturnValue(of([eventoExemplo()]));
    api.excluir.mockReturnValue(of(undefined));
    usuarioResponde(true);
    await criar();

    botao('Excluir').click();
    await fixture.whenStable();

    expect(api.excluir).toHaveBeenCalledWith('p-1');
    expect(notificacao.sucesso).toHaveBeenCalledWith('Processo excluído.');
    expect(router.navigate).toHaveBeenCalledWith(['/processos']);
  });

  it('processo inexistente mostra a mensagem da API', async () => {
    api.buscar.mockReturnValue(throwError(() => erroHttp(404, { detail: 'Processo não encontrado: p-1' })));

    await criar();

    expect(elemento.textContent).toContain('Processo não encontrado: p-1');
    expect(api.historico).not.toHaveBeenCalled();
  });

  describe('linha do tempo assíncrona', () => {
    it('consulta de novo até o evento da última alteração chegar', async () => {
      vi.useFakeTimers({ toFake: FAKE_INTERVALO });
      api.buscar.mockReturnValue(of(processoExemplo({ atualizadoEm: '2025-06-10T15:00:00Z' })));
      api.historico
        .mockReturnValueOnce(of([]))
        .mockReturnValueOnce(of([eventoExemplo({ ocorridoEm: '2025-06-10T15:00:00.001Z' })]));

      await criar();
      expect(elemento.textContent).toContain('Registrando a alteração na linha do tempo');

      await vi.advanceTimersByTimeAsync(1000);
      await fixture.whenStable();

      expect(api.historico).toHaveBeenCalledTimes(2);
      expect(elemento.textContent).toContain('Processo cadastrado');
      expect(elemento.textContent).not.toContain('Registrando a alteração');

      await vi.advanceTimersByTimeAsync(5000);
      expect(api.historico).toHaveBeenCalledTimes(2);
    });

    it('desiste após o limite de tentativas', async () => {
      vi.useFakeTimers({ toFake: FAKE_INTERVALO });
      api.buscar.mockReturnValue(of(processoExemplo()));
      api.historico.mockReturnValue(of([]));

      await criar();
      await vi.advanceTimersByTimeAsync(10_000);
      await fixture.whenStable();

      expect(api.historico).toHaveBeenCalledTimes(TENTATIVAS_HISTORICO);
      expect(elemento.textContent).not.toContain('Registrando a alteração');
    });

    it('botão de atualizar recarrega sob demanda e notifica falha', async () => {
      api.buscar.mockReturnValue(of(processoExemplo()));
      api.historico.mockReturnValueOnce(of([eventoExemplo()])).mockReturnValueOnce(throwError(() => erroHttp(503)));
      await criar();

      (elemento.querySelector('button[aria-label="Atualizar linha do tempo"]') as HTMLButtonElement).click();
      await fixture.whenStable();

      expect(notificacao.erro).toHaveBeenCalledTimes(1);
    });
  });
});
