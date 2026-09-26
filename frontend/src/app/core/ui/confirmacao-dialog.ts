import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';

export interface ConfirmacaoDados {
  titulo: string;
  mensagem: string;
  confirmar: string;
  perigoso?: boolean;
}

@Component({
  selector: 'app-confirmacao-dialog',
  imports: [MatDialogModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2 mat-dialog-title>{{ dados.titulo }}</h2>
    <mat-dialog-content>{{ dados.mensagem }}</mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Cancelar</button>
      <button
        mat-flat-button
        [class.botao-perigo]="dados.perigoso"
        [mat-dialog-close]="true"
        cdkFocusInitial
      >
        {{ dados.confirmar }}
      </button>
    </mat-dialog-actions>
  `,
})
export class ConfirmacaoDialog {
  protected readonly dados = inject<ConfirmacaoDados>(MAT_DIALOG_DATA);
}
