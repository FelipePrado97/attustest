import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { ApiError } from '../../core/api/api-error';
import { provideDatasBrasileiras } from '../../core/datas/data-brasileira.adapter';
import { MASCARA_DATA, dataParaIso, hoje, isoParaData } from '../../core/datas/datas';
import { MascaraDirective } from '../../core/mascara/mascara.directive';
import { Notificacao } from '../../core/notificacao/notificacao';
import { MASCARA_CNJ, gerarCnj } from '../cnj';
import { DadosProcesso, LIMITES, Processo } from '../processo.model';
import { ProcessoApi } from '../processo-api';
import { aplicarErrosDoServidor } from '../validacao/erros-servidor';
import { mensagemDeErro } from '../validacao/mensagem-erro';
import {
  cnjValidator,
  dataDistribuicaoValidator,
  dataMinimaDistribuicao,
  valorMonetarioValidator,
} from '../validacao/processo.validators';

type FormularioProcesso = FormGroup<{
  numeroCnj: FormControl<string>;
  assunto: FormControl<string>;
  parteContraria: FormControl<string>;
  valorCausa: FormControl<number | null>;
  dataDistribuicao: FormControl<Date | null>;
}>;

@Component({
  selector: 'app-processo-formulario',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressBarModule,
    MatTooltipModule,
    MascaraDirective,
  ],
  providers: [provideDatasBrasileiras()],
  templateUrl: './processo-formulario.html',
  styleUrl: './processo-formulario.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProcessoFormulario implements OnInit {
  private readonly api = inject(ProcessoApi);
  private readonly router = inject(Router);
  private readonly notificacao = inject(Notificacao);

  readonly id = input<string>();

  protected readonly edicao = computed(() => this.id() !== undefined);
  protected readonly dicaNumeroCnj = computed(() =>
    this.edicao()
      ? 'O número do processo não pode ser alterado.'
      : 'Digite o número que você já tem ou gere um automaticamente.',
  );
  protected readonly limites = LIMITES;
  protected readonly mascaraCnj = MASCARA_CNJ;
  protected readonly mascaraData = MASCARA_DATA;
  protected readonly dataMaxima = hoje();
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erroCarga = signal<ApiError | null>(null);

  private versao = 0;

  protected readonly form: FormularioProcesso = new FormGroup({
    numeroCnj: new FormControl('', { nonNullable: true, validators: [Validators.required, cnjValidator] }),
    assunto: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(LIMITES.assunto)],
    }),
    parteContraria: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(LIMITES.parteContraria)],
    }),
    valorCausa: new FormControl<number | null>(null, [
      Validators.required,
      valorMonetarioValidator(LIMITES.valorCausaMaximo),
    ]),
    dataDistribuicao: new FormControl<Date | null>(null, [Validators.required, dataDistribuicaoValidator()]),
  });

  private readonly numeroCnj = toSignal(this.form.controls.numeroCnj.valueChanges, { initialValue: '' });

  protected readonly dataMinima = computed(() => dataMinimaDistribuicao(this.numeroCnj()));

  constructor() {
    this.revalidarDataQuandoOAnoDoCnjMudar();
  }

  ngOnInit(): void {
    const id = this.id();
    if (id) {
      this.form.controls.numeroCnj.disable();
      this.carregar(id);
    }
  }

  protected erro(campo: keyof FormularioProcesso['controls']): string {
    return mensagemDeErro(this.form.controls[campo].errors);
  }

  protected gerarNumeroCnj(): void {
    const controle = this.form.controls.numeroCnj;
    controle.setValue(gerarCnj());
    controle.markAsDirty();
    controle.markAsTouched();
  }

  protected salvar(): void {
    const valores = this.form.getRawValue();
    if (this.form.invalid || valores.valorCausa === null || valores.dataDistribuicao === null) {
      this.form.markAllAsTouched();
      return;
    }
    this.salvando.set(true);
    const dados: DadosProcesso = {
      assunto: valores.assunto.trim(),
      parteContraria: valores.parteContraria.trim(),
      valorCausa: valores.valorCausa,
      dataDistribuicao: dataParaIso(valores.dataDistribuicao),
    };
    this.enviar(valores.numeroCnj, dados).subscribe({
      next: (processo) => {
        this.notificacao.sucesso(this.edicao() ? 'Processo atualizado.' : 'Processo cadastrado.');
        void this.router.navigate(['/processos', processo.id]);
      },
      error: (erro: unknown) => this.tratarErroAoSalvar(ApiError.de(erro)),
    });
  }

  protected recarregar(): void {
    const id = this.id();
    if (id) {
      this.carregar(id);
    }
  }

  private revalidarDataQuandoOAnoDoCnjMudar(): void {
    this.form.controls.numeroCnj.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.dataDistribuicao.updateValueAndValidity());
  }

  private enviar(numeroCnj: string, dados: DadosProcesso): Observable<Processo> {
    const id = this.id();
    return id ? this.api.atualizar(id, this.versao, dados) : this.api.cadastrar({ ...dados, numeroCnj });
  }

  private carregar(id: string): void {
    this.carregando.set(true);
    this.erroCarga.set(null);
    this.api.buscar(id).subscribe({
      next: (processo) => {
        this.preencher(processo);
        this.carregando.set(false);
      },
      error: (erro: unknown) => {
        this.erroCarga.set(ApiError.de(erro));
        this.carregando.set(false);
      },
    });
  }

  private preencher(processo: Processo): void {
    this.versao = processo.versao;
    this.form.reset({
      numeroCnj: processo.numeroCnj,
      assunto: processo.assunto,
      parteContraria: processo.parteContraria,
      valorCausa: processo.valorCausa,
      dataDistribuicao: isoParaData(processo.dataDistribuicao),
    });
  }

  private tratarErroAoSalvar(erro: ApiError): void {
    this.salvando.set(false);
    if (erro.conflito) {
      if (this.edicao()) {
        this.oferecerRecarregarAposConflitoDeVersao(erro);
      } else {
        this.marcarNumeroCnjDuplicado(erro);
      }
      return;
    }
    this.exibirErrosDeValidacao(erro);
  }

  private marcarNumeroCnjDuplicado(erro: ApiError): void {
    aplicarErrosDoServidor(this.form, [{ campo: 'numeroCnj', mensagem: erro.mensagem }]);
  }

  private oferecerRecarregarAposConflitoDeVersao(erro: ApiError): void {
    this.notificacao.erro(erro, { rotulo: 'Recarregar', executar: () => this.recarregar() });
  }

  private exibirErrosDeValidacao(erro: ApiError): void {
    const errosSemCampoNaTela = aplicarErrosDoServidor(this.form, erro.erros);
    if (erro.erros.length === 0 || errosSemCampoNaTela.length > 0) {
      this.notificacao.erro(erro);
    }
  }
}
