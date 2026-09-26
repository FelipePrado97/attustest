import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { firstValueFrom } from 'rxjs';

import { ConfirmacaoDados, ConfirmacaoDialog } from './confirmacao-dialog';

describe('ConfirmacaoDialog', () => {
  const dados: ConfirmacaoDados = {
    titulo: 'Excluir processo',
    mensagem: 'Deseja continuar?',
    confirmar: 'Excluir',
    perigoso: true,
  };

  function abrir() {
    const ref = TestBed.inject(MatDialog).open<ConfirmacaoDialog, ConfirmacaoDados, boolean>(ConfirmacaoDialog, { data: dados });
    TestBed.tick();
    const botoes = Array.from(document.querySelectorAll('mat-dialog-actions button')) as HTMLButtonElement[];
    return { ref, cancelar: botoes[0], confirmar: botoes[1] };
  }

  afterEach(() => TestBed.inject(MatDialog).closeAll());

  it('mostra título, mensagem e destaca a ação perigosa', () => {
    const { confirmar } = abrir();

    expect(document.querySelector('[mat-dialog-title]')?.textContent).toBe('Excluir processo');
    expect(document.querySelector('mat-dialog-content')?.textContent).toContain('Deseja continuar?');
    expect(confirmar.textContent?.trim()).toBe('Excluir');
    expect(confirmar.classList).toContain('botao-perigo');
  });

  it('confirmar fecha com true', async () => {
    const { ref, confirmar } = abrir();
    const resultado = firstValueFrom(ref.afterClosed());

    confirmar.click();

    expect(await resultado).toBe(true);
  });

  it('cancelar fecha com false', async () => {
    const { ref, cancelar } = abrir();
    const resultado = firstValueFrom(ref.afterClosed());

    cancelar.click();

    expect(await resultado).toBe(false);
  });
});
