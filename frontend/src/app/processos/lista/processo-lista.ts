import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { Observable, catchError, debounceTime, distinctUntilChanged, map, of, switchMap, tap } from 'rxjs';

import { ApiError } from '../../core/api/api-error';
import { ROTULO_STATUS } from '../processo.labels';
import { FiltroProcessos, Pagina, Processo, StatusProcesso, TODOS_STATUS } from '../processo.model';
import { ProcessoApi } from '../processo-api';
import { StatusChip } from '../ui/status-chip';

export const TAMANHOS_PAGINA = [10, 20, 50] as const;
const TAMANHO_PADRAO = TAMANHOS_PAGINA[0];
const PAUSA_NA_DIGITACAO_MS = 300;

@Component({
  selector: 'app-processo-lista',
  imports: [
    CurrencyPipe,
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatSelectModule,
    MatTableModule,
    MatTooltipModule,
    StatusChip,
  ],
  templateUrl: './processo-lista.html',
  styleUrl: './processo-lista.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcessoLista {
  private readonly api = inject(ProcessoApi);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly colunas = ['numeroCnj', 'assunto', 'parteContraria', 'valorCausa', 'status', 'atualizadoEm', 'acoes'];
  protected readonly todosStatus = TODOS_STATUS;
  protected readonly rotuloStatus = ROTULO_STATUS;
  protected readonly tamanhosPagina = TAMANHOS_PAGINA;

  protected readonly termo = new FormControl('', { nonNullable: true });
  protected readonly status = new FormControl<StatusProcesso | null>(null);

  protected readonly filtro = signal<FiltroProcessos>({ pagina: 0, tamanho: TAMANHO_PADRAO });
  protected readonly resultado = signal<Pagina<Processo> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<ApiError | null>(null);

  constructor() {
    this.carregarSempreQueAUrlMudar();
    this.buscarAoTerminarDeDigitar();
    this.filtrarAoEscolherStatus();
  }

  protected mudarPagina(evento: PageEvent): void {
    this.navegar({ pagina: evento.pageIndex, tamanho: evento.pageSize });
  }

  protected limparFiltros(): void {
    this.navegar({ termo: undefined, status: undefined, pagina: 0 });
  }

  protected tentarNovamente(): void {
    this.carregar(this.filtro()).subscribe((pagina) => this.resultado.set(pagina));
  }

  private carregarSempreQueAUrlMudar(): void {
    this.route.queryParamMap
      .pipe(
        map(filtroDaUrl),
        tap((filtro) => this.sincronizarCampos(filtro)),
        switchMap((filtro) => this.carregar(filtro)),
        takeUntilDestroyed(),
      )
      .subscribe((pagina) => this.resultado.set(pagina));
  }

  private buscarAoTerminarDeDigitar(): void {
    this.termo.valueChanges
      .pipe(debounceTime(PAUSA_NA_DIGITACAO_MS), distinctUntilChanged(), takeUntilDestroyed())
      .subscribe((termo) => this.navegar({ termo: termo.trim() || undefined, pagina: 0 }));
  }

  private filtrarAoEscolherStatus(): void {
    this.status.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((status) => this.navegar({ status: status ?? undefined, pagina: 0 }));
  }

  private carregar(filtro: FiltroProcessos): Observable<Pagina<Processo> | null> {
    this.filtro.set(filtro);
    this.carregando.set(true);
    this.erro.set(null);
    return this.api.listar(filtro).pipe(
      catchError((erro: unknown) => {
        this.erro.set(ApiError.de(erro));
        return of(null);
      }),
      tap(() => this.carregando.set(false)),
    );
  }

  private sincronizarCampos(filtro: FiltroProcessos): void {
    if (this.termo.value.trim() !== (filtro.termo ?? '')) {
      this.termo.setValue(filtro.termo ?? '', { emitEvent: false });
    }
    this.status.setValue(filtro.status ?? null, { emitEvent: false });
  }

  private navegar(mudancas: Partial<FiltroProcessos>): void {
    const proximo = { ...this.filtro(), ...mudancas };
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        termo: proximo.termo || null,
        status: proximo.status ?? null,
        pagina: proximo.pagina || null,
        tamanho: proximo.tamanho === TAMANHO_PADRAO ? null : proximo.tamanho,
      },
    });
  }
}

export function filtroDaUrl(params: ParamMap): FiltroProcessos {
  const status = params.get('status');
  const pagina = Number(params.get('pagina') ?? 0);
  const tamanho = Number(params.get('tamanho') ?? TAMANHO_PADRAO);
  return {
    termo: params.get('termo') ?? undefined,
    status: TODOS_STATUS.includes(status as StatusProcesso) ? (status as StatusProcesso) : undefined,
    pagina: Number.isInteger(pagina) && pagina >= 0 ? pagina : 0,
    tamanho: (TAMANHOS_PAGINA as readonly number[]).includes(tamanho) ? tamanho : TAMANHO_PADRAO,
  };
}
