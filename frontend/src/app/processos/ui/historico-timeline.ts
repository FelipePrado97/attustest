import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

import { ICONE_EVENTO, ROTULO_EVENTO } from '../processo.labels';
import { RegistroHistorico } from '../processo.model';

@Component({
  selector: 'app-historico-timeline',
  imports: [DatePipe, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (registros().length === 0) {
      <p class="vazio">Nenhum evento registrado ainda.</p>
    } @else {
      <ol class="timeline">
        @for (registro of registros(); track registro.eventoId) {
          <li class="evento">
            <mat-icon class="icone" aria-hidden="true">{{ icone[registro.tipo] }}</mat-icon>
            <div>
              <div class="titulo">{{ rotulo[registro.tipo] }}</div>
              <div class="descricao">{{ registro.descricao }}</div>
              <time class="data" [attr.datetime]="registro.ocorridoEm">
                {{ registro.ocorridoEm | date: 'dd/MM/yyyy HH:mm:ss' }}
              </time>
            </div>
          </li>
        }
      </ol>
    }
  `,
  styles: `
    .timeline { list-style: none; margin: 0; padding: 0; }
    .evento {
      display: flex;
      gap: 12px;
      padding: 12px 0;
      border-bottom: 1px solid var(--borda-suave);
    }
    .evento:last-child { border-bottom: none; }
    .icone { color: var(--mat-sys-primary); flex-shrink: 0; }
    .titulo { font-weight: 500; }
    .descricao { color: var(--texto-secundario); }
    .data { font-size: 12px; color: var(--texto-secundario); }
    .vazio { color: var(--texto-secundario); margin: 0; }
  `,
})
export class HistoricoTimeline {
  readonly registros = input.required<RegistroHistorico[]>();

  protected readonly icone = ICONE_EVENTO;
  protected readonly rotulo = ROTULO_EVENTO;
}
