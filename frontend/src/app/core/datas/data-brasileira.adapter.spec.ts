import { TestBed } from '@angular/core/testing';
import { DateAdapter } from '@angular/material/core';

import { FORMATOS_DATA_BR, provideDatasBrasileiras } from './data-brasileira.adapter';
import { dataParaIso } from './datas';

describe('DataBrasileiraAdapter', () => {
  let adapter: DateAdapter<Date>;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: provideDatasBrasileiras() });
    adapter = TestBed.inject(DateAdapter);
  });

  const parse = (texto: string) => adapter.parse(texto, null) as Date;

  it.each([
    ['15/03/2024', '2024-03-15'],
    ['15032024', '2024-03-15'],
    [' 01/01/1900 ', '1900-01-01'],
  ])('interpreta "%s" como %s', (texto, iso) => {
    expect(dataParaIso(parse(texto))).toBe(iso);
  });

  it('mantém o ano digitado literalmente (0004 não vira 1904)', () => {
    expect(parse('11/04/0004').getFullYear()).toBe(4);
  });

  it.each(['31/02/2024', '32/01/2024', '15/13/2024', '2024-03-15', '15/3/24', 'abc'])('"%s" é inválida', (texto) => {
    expect(adapter.isValid(parse(texto))).toBe(false);
  });

  it('texto vazio é ausência de data, não data inválida', () => {
    expect(adapter.parse('   ', null)).toBeNull();
  });

  it('valores que não são texto seguem o comportamento padrão', () => {
    const data = new Date(2024, 2, 15);
    expect(adapter.parse(data.getTime(), null)?.getTime()).toBe(data.getTime());
  });

  it('exibe no campo como dd/mm/aaaa, com o ano sempre em 4 dígitos', () => {
    expect(adapter.format(new Date(2024, 2, 5), FORMATOS_DATA_BR.display.dateInput)).toBe('05/03/2024');
    expect(adapter.format(parse('11/04/0004'), FORMATOS_DATA_BR.display.dateInput)).toBe('11/04/0004');
  });

  it('outros formatos (cabeçalho do calendário) seguem o Intl em pt-BR', () => {
    expect(adapter.format(new Date(2024, 0, 1), FORMATOS_DATA_BR.display.monthYearLabel).toLowerCase()).toContain('2024');
  });
});
