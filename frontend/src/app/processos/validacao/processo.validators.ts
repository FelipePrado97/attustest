import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import { criarData, dataParaIso, hoje } from '../../core/datas/datas';
import { somenteDigitos } from '../../core/mascara/mascara';
import { TOTAL_DIGITOS_CNJ, anoAjuizamento, cnjValido } from '../cnj';

export const DATA_MINIMA_DISTRIBUICAO = criarData(1900, 1, 1);

export const cnjValidator: ValidatorFn = (control: AbstractControl<string | null>): ValidationErrors | null => {
  const valor = control.value ?? '';
  if (valor.trim() === '') {
    return null;
  }
  if (somenteDigitos(valor).length !== TOTAL_DIGITOS_CNJ) {
    return { cnjIncompleto: true };
  }
  return cnjValido(valor) ? null : { cnjDigitoVerificador: true };
};

export function dataMinimaDistribuicao(numeroCnj: string | null | undefined): Date {
  const ano = anoAjuizamento(numeroCnj);
  return ano !== null && ano > DATA_MINIMA_DISTRIBUICAO.getFullYear() ? criarData(ano, 1, 1) : DATA_MINIMA_DISTRIBUICAO;
}

export function dataDistribuicaoValidator(obterHoje: () => Date = hoje): ValidatorFn {
  return (control: AbstractControl<Date | null>): ValidationErrors | null => {
    const data = control.value;
    if (!(data instanceof Date) || Number.isNaN(data.getTime())) {
      return null;
    }
    const iso = dataParaIso(data);
    if (iso < dataParaIso(DATA_MINIMA_DISTRIBUICAO)) {
      return { dataAnteriorMinima: true };
    }
    const ano = anoAjuizamento(control.parent?.get('numeroCnj')?.value as string | undefined);
    if (ano !== null && data.getFullYear() < ano) {
      return { dataAnteriorAjuizamento: { ano } };
    }
    return iso > dataParaIso(obterHoje()) ? { dataFutura: true } : null;
  };
}

export function valorMonetarioValidator(maximo: number): ValidatorFn {
  return (control: AbstractControl<number | null>): ValidationErrors | null => {
    const valor = control.value;
    if (valor === null || valor === undefined || Number.isNaN(valor)) {
      return null;
    }
    if (valor < 0) {
      return { valorNegativo: true };
    }
    if (valor > maximo) {
      return { valorMaximo: true };
    }
    return casasDecimais(valor) > 2 ? { casasDecimais: true } : null;
  };
}

function casasDecimais(valor: number): number {
  const [, decimais = ''] = String(valor).split('.');
  return decimais.length;
}
