import { Injectable, Provider } from '@angular/core';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MatDateFormats, NativeDateAdapter } from '@angular/material/core';

import { criarData, dataExiste } from './datas';

const DATA_DIGITADA = /^(\d{2})\/?(\d{2})\/?(\d{4})$/;

@Injectable()
export class DataBrasileiraAdapter extends NativeDateAdapter {
  override parse(valor: unknown, formato?: unknown): Date | null {
    if (typeof valor !== 'string') {
      return super.parse(valor, formato);
    }
    const texto = valor.trim();
    if (texto === '') {
      return null;
    }
    const partes = DATA_DIGITADA.exec(texto);
    if (!partes) {
      return this.invalid();
    }
    const [dia, mes, ano] = partes.slice(1).map(Number);
    return dataExiste(ano, mes, dia) ? criarData(ano, mes, dia) : this.invalid();
  }

  override format(data: Date, formato: object): string {
    return formato === FORMATOS_DATA_BR.display.dateInput
      ? formatarComAnoDeQuatroDigitos(data)
      : super.format(data, formato);
  }
}

function formatarComAnoDeQuatroDigitos(data: Date): string {
  const dia = String(data.getDate()).padStart(2, '0');
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  return `${dia}/${mes}/${String(data.getFullYear()).padStart(4, '0')}`;
}

export const FORMATOS_DATA_BR: MatDateFormats = {
  parse: { dateInput: null },
  display: {
    dateInput: { day: '2-digit', month: '2-digit', year: 'numeric' },
    monthYearLabel: { month: 'short', year: 'numeric' },
    dateA11yLabel: { day: 'numeric', month: 'long', year: 'numeric' },
    monthYearA11yLabel: { month: 'long', year: 'numeric' },
  },
};

export function provideDatasBrasileiras(): Provider[] {
  return [
    { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
    { provide: DateAdapter, useClass: DataBrasileiraAdapter },
    { provide: MAT_DATE_FORMATS, useValue: FORMATOS_DATA_BR },
  ];
}
