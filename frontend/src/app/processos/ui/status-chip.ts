import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { ROTULO_STATUS } from '../processo.labels';
import { StatusProcesso } from '../processo.model';

@Component({
  selector: 'app-status-chip',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="status-chip" [class]="'status-' + status().toLowerCase()">{{ rotulo() }}</span>`,
  styles: `
    .status-chip {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      line-height: 20px;
      white-space: nowrap;
    }
    .status-ativo { background: var(--status-ativo-bg); color: var(--status-ativo-fg); }
    .status-suspenso { background: var(--status-suspenso-bg); color: var(--status-suspenso-fg); }
    .status-arquivado { background: var(--status-arquivado-bg); color: var(--status-arquivado-fg); }
  `,
})
export class StatusChip {
  readonly status = input.required<StatusProcesso>();

  protected readonly rotulo = computed(() => ROTULO_STATUS[this.status()]);
}
