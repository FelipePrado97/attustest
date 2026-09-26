import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { erroHttp, processoExemplo } from '../../../testing/processo.fixture';
import { AcaoNotificacao, Notificacao } from '../../core/notificacao/notificacao';
import { provideLocalizacao } from '../../core/localizacao';
import { anoAjuizamento } from '../cnj';
import { ProcessoApi } from '../processo-api';
import { ProcessoFormulario } from './processo-formulario';

describe('ProcessoFormulario', () => {
  let fixture: ComponentFixture<ProcessoFormulario>;
  let elemento: HTMLElement;
  let router: Router;
  const api = {
    buscar: vi.fn(),
    cadastrar: vi.fn(),
    atualizar: vi.fn(),
  };
  const notificacao = { sucesso: vi.fn(), erro: vi.fn() };

  beforeEach(() => {
    vi.resetAllMocks();
    TestBed.configureTestingModule({
      imports: [ProcessoFormulario],
      providers: [
        provideRouter([]),
        provideLocalizacao(),
        { provide: ProcessoApi, useValue: api },
        { provide: Notificacao, useValue: notificacao },
      ],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  async function criar(id?: string): Promise<void> {
    fixture = TestBed.createComponent(ProcessoFormulario);
    if (id) {
      fixture.componentRef.setInput('id', id);
    }
    elemento = fixture.nativeElement;
    await fixture.whenStable();
  }

  function campo(nome: string): HTMLInputElement {
    return elemento.querySelector(`[formcontrolname="${nome}"]`) as HTMLInputElement;
  }

  function digitar(nome: string, valor: string): void {
    const input = campo(nome);
    input.value = valor;
    input.dispatchEvent(new Event('input'));
  }

  async function preencherValido(): Promise<void> {
    digitar('numeroCnj', '00012347120248260100');
    digitar('assunto', '  Execução fiscal  ');
    digitar('parteContraria', 'Empresa Exemplo');
    digitar('valorCausa', '1500.5');
    digitar('dataDistribuicao', '15/03/2024');
    await fixture.whenStable();
  }

  async function salvar(): Promise<void> {
    (elemento.querySelector('button[type="submit"]') as HTMLButtonElement).click();
    await fixture.whenStable();
  }

  function mensagensDeErro(): string[] {
    return Array.from(elemento.querySelectorAll('mat-error')).map((e) => e.textContent?.trim() ?? '');
  }

  describe('cadastro', () => {
    beforeEach(() => criar());

    it('aplica a máscara do CNJ enquanto o usuário digita', async () => {
      digitar('numeroCnj', '00012347120248260100');
      await fixture.whenStable();

      expect(campo('numeroCnj').value).toBe('0001234-71.2024.8.26.0100');
    });

    it('não envia formulário inválido e mostra as mensagens de cada campo', async () => {
      digitar('numeroCnj', '0001234-72.2024.8.26.0100');
      await salvar();

      expect(api.cadastrar).not.toHaveBeenCalled();
      expect(mensagensDeErro()).toEqual([
        'Número CNJ inválido: dígito verificador não confere.',
        'Campo obrigatório.',
        'Campo obrigatório.',
        'Campo obrigatório.',
        'Campo obrigatório.',
      ]);
    });

    it('cadastra com os textos normalizados e abre o detalhe', async () => {
      api.cadastrar.mockReturnValue(of(processoExemplo({ id: 'novo-id' })));
      await preencherValido();

      await salvar();

      expect(api.cadastrar).toHaveBeenCalledWith({
        numeroCnj: '0001234-71.2024.8.26.0100',
        assunto: 'Execução fiscal',
        parteContraria: 'Empresa Exemplo',
        valorCausa: 1500.5,
        dataDistribuicao: '2024-03-15',
      });
      expect(notificacao.sucesso).toHaveBeenCalledWith('Processo cadastrado.');
      expect(router.navigate).toHaveBeenCalledWith(['/processos', 'novo-id']);
    });

    describe('gerador de número CNJ', () => {
      const botaoGerar = () => elemento.querySelector('button[aria-label="Gerar um número CNJ válido"]') as HTMLButtonElement | null;

      it('preenche um número válido do ano corrente, já com a máscara', async () => {
        botaoGerar()?.click();
        await fixture.whenStable();

        const numero = campo('numeroCnj').value;
        expect(numero).toMatch(/^\d{7}-\d{2}\.\d{4}\.\d\.\d{2}\.\d{4}$/);
        expect(anoAjuizamento(numero)).toBe(new Date().getFullYear());
        expect(mensagensDeErro()).toEqual([]);
      });

      it('não submete o formulário ao gerar', async () => {
        botaoGerar()?.click();
        await fixture.whenStable();

        expect(api.cadastrar).not.toHaveBeenCalled();
      });

      it('o número gerado ajusta o mínimo do calendário para o ano corrente', async () => {
        botaoGerar()?.click();
        await fixture.whenStable();

        expect(campo('dataDistribuicao').getAttribute('min')).toBe(`${new Date().getFullYear()}-01-01`);
      });
    });

    describe('data de distribuição', () => {
      it('digitada sem o calendário recebe a máscara e ignora letras', async () => {
        digitar('dataDistribuicao', '1a5b0c3d2024');
        await fixture.whenStable();

        expect(campo('dataDistribuicao').value).toBe('15/03/2024');
      });

      it('ano absurdo digitado (0004) é rejeitado pelo piso de 1900', async () => {
        await preencherValido();
        digitar('dataDistribuicao', '11/04/0004');
        await salvar();

        expect(api.cadastrar).not.toHaveBeenCalled();
        expect(mensagensDeErro()).toEqual(['A data não pode ser anterior a 01/01/1900.']);
      });

      it('data anterior ao ano de ajuizamento do CNJ é rejeitada', async () => {
        await preencherValido();
        digitar('dataDistribuicao', '31/12/2023');
        await salvar();

        expect(mensagensDeErro()).toEqual(['A data não pode ser anterior ao ano de ajuizamento do processo (2024).']);
      });

      it('data impossível mostra o formato esperado', async () => {
        await preencherValido();
        digitar('dataDistribuicao', '31/02/2024');
        await salvar();

        expect(mensagensDeErro()).toEqual(['Data inválida. Use o formato dd/mm/aaaa.']);
      });

      it('trocar o CNJ revalida a data e ajusta o mínimo do calendário', async () => {
        await preencherValido();
        expect(campo('dataDistribuicao').getAttribute('min')).toBe('2024-01-01');

        digitar('numeroCnj', '0001000-55.2025.8.26.0100');
        await salvar();

        expect(campo('dataDistribuicao').getAttribute('min')).toBe('2025-01-01');
        expect(mensagensDeErro()).toEqual(['A data não pode ser anterior ao ano de ajuizamento do processo (2025).']);
        expect(api.cadastrar).not.toHaveBeenCalled();
      });
    });

    it('número já cadastrado (409) aparece no próprio campo CNJ', async () => {
      api.cadastrar.mockReturnValue(throwError(() => erroHttp(409, { detail: 'Já existe processo cadastrado com o número X' })));
      await preencherValido();

      await salvar();

      expect(mensagensDeErro()).toEqual(['Já existe processo cadastrado com o número X']);
      expect(notificacao.erro).not.toHaveBeenCalled();
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('erros de campo devolvidos pela API vão para os controles', async () => {
      api.cadastrar.mockReturnValue(
        throwError(() => erroHttp(400, { detail: 'Inválido', erros: [{ campo: 'assunto', mensagem: 'Deve ter no máximo 200 caracteres' }] })),
      );
      await preencherValido();

      await salvar();

      expect(mensagensDeErro()).toEqual(['Deve ter no máximo 200 caracteres']);
      expect(notificacao.erro).not.toHaveBeenCalled();
    });

    it('erro sem detalhe de campo é notificado e libera o botão de salvar', async () => {
      api.cadastrar.mockReturnValue(throwError(() => erroHttp(500, { detail: 'Erro interno' })));
      await preencherValido();

      await salvar();

      expect(notificacao.erro).toHaveBeenCalled();
      expect((elemento.querySelector('button[type="submit"]') as HTMLButtonElement).disabled).toBe(false);
    });
  });

  describe('edição', () => {
    it('carrega o processo, bloqueia o CNJ e envia a versão lida', async () => {
      api.buscar.mockReturnValue(of(processoExemplo({ id: 'p-9', versao: 4 })));
      api.atualizar.mockReturnValue(of(processoExemplo({ id: 'p-9', versao: 5 })));
      await criar('p-9');

      expect(campo('numeroCnj').disabled).toBe(true);
      expect(elemento.querySelector('button[aria-label="Gerar um número CNJ válido"]')).toBeNull();
      expect(campo('assunto').value).toBe('Execução fiscal - IPTU 2021');
      expect(campo('dataDistribuicao').value).toBe('15/03/2024');

      digitar('assunto', 'Assunto revisado');
      await salvar();

      expect(api.atualizar).toHaveBeenCalledWith('p-9', 4, {
        assunto: 'Assunto revisado',
        parteContraria: 'Empresa Exemplo Ltda',
        valorCausa: 15230.5,
        dataDistribuicao: '2024-03-15',
      });
      expect(notificacao.sucesso).toHaveBeenCalledWith('Processo atualizado.');
    });

    it('conflito de versão (409) oferece recarregar os dados atuais', async () => {
      api.buscar.mockReturnValue(of(processoExemplo({ id: 'p-9', versao: 4 })));
      api.atualizar.mockReturnValue(throwError(() => erroHttp(409, { detail: 'Alterado por outro usuário' })));
      await criar('p-9');

      await salvar();

      expect(notificacao.erro).toHaveBeenCalledTimes(1);
      const acao = notificacao.erro.mock.calls[0][1] as AcaoNotificacao;
      expect(acao.rotulo).toBe('Recarregar');

      acao.executar();
      expect(api.buscar).toHaveBeenCalledTimes(2);
    });

    it('processo inexistente mostra mensagem e link para a lista', async () => {
      api.buscar.mockReturnValue(throwError(() => erroHttp(404, { detail: 'Processo não encontrado' })));
      await criar('nao-existe');

      expect(elemento.textContent).toContain('Processo não encontrado');
      const voltar = elemento.querySelector('[role="alert"] a') as HTMLAnchorElement;
      expect(voltar.textContent).toContain('Voltar para a lista');
      expect(voltar.getAttribute('href')).toBe('/processos');
      expect(elemento.querySelector('form')).toBeNull();
    });

    it('falha temporária ao carregar permite tentar de novo', async () => {
      api.buscar.mockReturnValueOnce(throwError(() => erroHttp(503))).mockReturnValueOnce(of(processoExemplo()));
      await criar('p-1');

      (Array.from(elemento.querySelectorAll('button')).find((b) => b.textContent?.includes('Tentar novamente')) as HTMLButtonElement).click();
      await fixture.whenStable();

      expect(elemento.querySelector('form')).not.toBeNull();
    });
  });
});
