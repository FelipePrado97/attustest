import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { EMPTY, Observable, catchError, filter, finalize, interval, of, startWith, switchMap, take, takeWhile } from 'rxjs';

import { ApiError } from '../../core/api/api-error';
import { Notificacao } from '../../core/notificacao/notificacao';
import { ConfirmacaoDados, ConfirmacaoDialog } from '../../core/ui/confirmacao-dialog';
import { ROTULO_STATUS, rotuloAcaoStatus } from '../processo.labels';
import { Processo, RegistroHistorico, StatusProcesso } from '../processo.model';
import { ProcessoApi } from '../processo-api';
import { HistoricoTimeline } from '../ui/historico-timeline';
import { StatusChip } from '../ui/status-chip';

export const TENTATIVAS_HISTORICO = 6;
const INTERVALO_CONSULTA_HISTORICO_MS = 1000;
const CONSULTA_IMEDIATA = -1;
const SEM_INTERROMPER_O_USUARIO = EMPTY;

export function historicoPendente(processo: Processo, registros: readonly RegistroHistorico[]): boolean {
  const ultimaAlteracao = Date.parse(processo.atualizadoEm);
  return !registros.some((registro) => Date.parse(registro.ocorridoEm) >= ultimaAlteracao);
}

@Component({
  selector: 'app-processo-detalhe',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatTooltipModule,
    HistoricoTimeline,
    StatusChip,
  ],
  templateUrl: './processo-detalhe.html',
  styleUrl: './processo-detalhe.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcessoDetalhe implements OnInit {
  private readonly api = inject(ProcessoApi);
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly notificacao = inject(Notificacao);
  private readonly destroyRef = inject(DestroyRef);

  readonly id = input.required<string>();

  protected readonly processo = signal<Processo | null>(null);
  protected readonly historico = signal<RegistroHistorico[]>([]);
  protected readonly carregando = signal(true);
  protected readonly erroCarga = signal<ApiError | null>(null);
  protected readonly processando = signal(false);
  protected readonly atualizandoHistorico = signal(false);
  protected readonly rotuloAcao = rotuloAcaoStatus;

  ngOnInit(): void {
    this.carregar();
  }

  protected carregar(): void {
    this.carregando.set(true);
    this.erroCarga.set(null);
    this.api.buscar(this.id()).subscribe({
      next: (processo) => {
        this.processo.set(processo);
        this.carregando.set(false);
        this.sincronizarHistoricoAteRefletirUltimaAlteracao();
      },
      error: (erro: unknown) => {
        this.erroCarga.set(ApiError.de(erro));
        this.carregando.set(false);
      },
    });
  }

  protected recarregarHistorico(): void {
    this.api.historico(this.id()).subscribe({
      next: (registros) => this.historico.set(registros),
      error: (erro: unknown) => this.notificacao.erro(erro),
    });
  }

  protected alterarStatus(destino: StatusProcesso): void {
    const processo = this.processo();
    if (!processo) {
      return;
    }
    this.confirmarSeForArquivamento(processo, destino)
      .pipe(switchMap(() => this.executarEscrita(this.api.alterarStatus(processo.id, processo.versao, destino))))
      .subscribe((atualizado) => {
        this.processo.set(atualizado);
        this.notificacao.sucesso(`Status alterado para ${ROTULO_STATUS[atualizado.status].toLowerCase()}.`);
        this.sincronizarHistoricoAteRefletirUltimaAlteracao();
      });
  }

  protected excluir(): void {
    const processo = this.processo();
    if (!processo) {
      return;
    }
    this.confirmar({
      titulo: 'Excluir processo',
      mensagem: `O processo ${processo.numeroCnj} será excluído. O histórico é mantido para auditoria. Deseja continuar?`,
      confirmar: 'Excluir',
      perigoso: true,
    })
      .pipe(switchMap(() => this.executarEscrita(this.api.excluir(processo.id))))
      .subscribe(() => {
        this.notificacao.sucesso('Processo excluído.');
        void this.router.navigate(['/processos']);
      });
  }

  private confirmarSeForArquivamento(processo: Processo, destino: StatusProcesso): Observable<boolean> {
    if (destino !== 'ARQUIVADO') {
      return of(true);
    }
    return this.confirmar({
      titulo: 'Arquivar processo',
      mensagem: `Processos arquivados não podem ser editados até serem desarquivados. Arquivar ${processo.numeroCnj}?`,
      confirmar: 'Arquivar',
    });
  }

  private confirmar(dados: ConfirmacaoDados): Observable<boolean> {
    return this.dialog
      .open<ConfirmacaoDialog, ConfirmacaoDados, boolean>(ConfirmacaoDialog, { data: dados, width: '440px' })
      .afterClosed()
      .pipe(filter((confirmado): confirmado is true => confirmado === true));
  }

  private executarEscrita<T>(operacao: Observable<T>): Observable<T> {
    this.processando.set(true);
    return operacao.pipe(
      catchError((erro: unknown) => {
        this.notificarFalhaNaEscrita(ApiError.de(erro));
        return EMPTY;
      }),
      finalize(() => this.processando.set(false)),
      takeUntilDestroyed(this.destroyRef),
    );
  }

  private notificarFalhaNaEscrita(erro: ApiError): void {
    if (erro.conflito) {
      this.oferecerRecarregarAposConflitoDeVersao(erro);
    } else {
      this.notificacao.erro(erro);
    }
  }

  private oferecerRecarregarAposConflitoDeVersao(erro: ApiError): void {
    this.notificacao.erro(erro, { rotulo: 'Recarregar', executar: () => this.carregar() });
  }

  private sincronizarHistoricoAteRefletirUltimaAlteracao(): void {
    const processo = this.processo();
    if (!processo) {
      return;
    }
    interval(INTERVALO_CONSULTA_HISTORICO_MS)
      .pipe(
        startWith(CONSULTA_IMEDIATA),
        take(TENTATIVAS_HISTORICO),
        switchMap(() => this.api.historico(processo.id)),
        takeWhile((registros) => historicoPendente(processo, registros), true),
        catchError(() => SEM_INTERROMPER_O_USUARIO),
        finalize(() => this.atualizandoHistorico.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((registros) => {
        this.historico.set(registros);
        this.atualizandoHistorico.set(historicoPendente(processo, registros));
      });
  }
}
