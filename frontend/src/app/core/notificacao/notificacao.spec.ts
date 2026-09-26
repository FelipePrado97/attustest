import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';

import { erroHttp } from '../../../testing/processo.fixture';
import { Notificacao } from './notificacao';

describe('Notificacao', () => {
  const acaoClicada = new Subject<void>();
  const snackBar = { open: vi.fn() };
  let notificacao: Notificacao;

  beforeEach(() => {
    vi.resetAllMocks();
    snackBar.open.mockReturnValue({ onAction: () => acaoClicada.asObservable() });
    TestBed.configureTestingModule({ providers: [{ provide: MatSnackBar, useValue: snackBar }] });
    notificacao = TestBed.inject(Notificacao);
  });

  it('sucesso mostra a mensagem por alguns segundos', () => {
    notificacao.sucesso('Processo cadastrado.');

    expect(snackBar.open).toHaveBeenCalledWith('Processo cadastrado.', 'OK', expect.objectContaining({ duration: 4000 }));
  });

  it('erro inclui o código de correlação para o suporte', () => {
    notificacao.erro(erroHttp(500, { detail: 'Erro interno' }));

    expect(snackBar.open).toHaveBeenCalledWith('Erro interno (código: cid-teste)', 'Fechar', expect.any(Object));
  });

  it('erro com ação executa o callback quando o usuário clica', () => {
    const executar = vi.fn();

    notificacao.erro(erroHttp(409, { detail: 'Conflito' }), { rotulo: 'Recarregar', executar });
    acaoClicada.next();

    expect(snackBar.open).toHaveBeenCalledWith(expect.any(String), 'Recarregar', expect.any(Object));
    expect(executar).toHaveBeenCalledTimes(1);
  });
});
