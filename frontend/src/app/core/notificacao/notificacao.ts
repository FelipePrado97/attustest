import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import { ApiError } from '../api/api-error';

export interface AcaoNotificacao {
  rotulo: string;
  executar: () => void;
}

@Injectable({ providedIn: 'root' })
export class Notificacao {
  private readonly snackBar = inject(MatSnackBar);

  sucesso(mensagem: string): void {
    this.snackBar.open(mensagem, 'OK', { duration: 4000, panelClass: 'notificacao-sucesso' });
  }

  erro(erro: unknown, acao?: AcaoNotificacao): void {
    const apiError = ApiError.de(erro);
    const sufixo = apiError.correlationId ? ` (código: ${apiError.correlationId})` : '';
    const referencia = this.snackBar.open(apiError.mensagem + sufixo, acao?.rotulo ?? 'Fechar', {
      duration: 10000,
      panelClass: 'notificacao-erro',
    });
    if (acao) {
      referencia.onAction().subscribe(() => acao.executar());
    }
  }
}
