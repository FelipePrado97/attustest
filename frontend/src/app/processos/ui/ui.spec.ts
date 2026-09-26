import { TestBed } from '@angular/core/testing';

import { eventoExemplo } from '../../../testing/processo.fixture';
import { HistoricoTimeline } from './historico-timeline';
import { StatusChip } from './status-chip';

describe('StatusChip', () => {
  it.each([
    ['ATIVO', 'Ativo', 'status-ativo'],
    ['SUSPENSO', 'Suspenso', 'status-suspenso'],
    ['ARQUIVADO', 'Arquivado', 'status-arquivado'],
  ] as const)('%s é exibido como "%s"', async (status, rotulo, classe) => {
    const fixture = TestBed.createComponent(StatusChip);
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();

    const chip = (fixture.nativeElement as HTMLElement).querySelector('span') as HTMLElement;
    expect(chip.textContent).toBe(rotulo);
    expect(chip.classList).toContain('status-chip');
    expect(chip.classList).toContain(classe);
  });
});

describe('HistoricoTimeline', () => {
  it('lista os eventos com rótulo, descrição e data', async () => {
    const fixture = TestBed.createComponent(HistoricoTimeline);
    fixture.componentRef.setInput('registros', [
      eventoExemplo({ eventoId: 'e-2', tipo: 'STATUS_ALTERADO', descricao: 'Status alterado de ATIVO para SUSPENSO' }),
      eventoExemplo(),
    ]);
    await fixture.whenStable();

    const itens = (fixture.nativeElement as HTMLElement).querySelectorAll('li');
    expect(itens.length).toBe(2);
    expect(itens[0].textContent).toContain('Mudança de status');
    expect(itens[0].textContent).toContain('Status alterado de ATIVO para SUSPENSO');
    expect(itens[1].querySelector('time')?.getAttribute('datetime')).toBe('2025-06-10T15:00:00.100Z');
  });

  it('sem eventos mostra aviso', async () => {
    const fixture = TestBed.createComponent(HistoricoTimeline);
    fixture.componentRef.setInput('registros', []);
    await fixture.whenStable();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Nenhum evento registrado ainda.');
  });
});
