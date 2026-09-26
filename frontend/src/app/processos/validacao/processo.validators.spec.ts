import { FormControl, FormGroup } from '@angular/forms';

import { criarData, dataParaIso, hoje } from '../../core/datas/datas';
import { gerarCnj } from '../cnj';
import {
  cnjValidator,
  dataDistribuicaoValidator,
  dataMinimaDistribuicao,
  valorMonetarioValidator,
} from './processo.validators';

describe('validações de processo', () => {
  describe('cnjValidator', () => {
    const validar = (valor: string | null) => cnjValidator(new FormControl(valor));

    it('deixa vazio para o Validators.required', () => {
      expect(validar('')).toBeNull();
      expect(validar(null)).toBeNull();
    });

    it('indica número incompleto', () => {
      expect(validar('0001234-71.2024')).toEqual({ cnjIncompleto: true });
    });

    it('indica dígito verificador inválido', () => {
      expect(validar('0001234-72.2024.8.26.0100')).toEqual({ cnjDigitoVerificador: true });
    });

    it('aceita número válido', () => {
      expect(validar('0001234-71.2024.8.26.0100')).toBeNull();
    });
  });

  describe('dataMinimaDistribuicao', () => {
    it('mínimo é 1º de janeiro do ano de ajuizamento', () => {
      expect(dataParaIso(dataMinimaDistribuicao('0001234-71.2024.8.26.0100'))).toBe('2024-01-01');
    });

    it('sem CNJ válido (ou com ano antigo) o mínimo é 01/01/1900', () => {
      expect(dataParaIso(dataMinimaDistribuicao(''))).toBe('1900-01-01');
      expect(dataParaIso(dataMinimaDistribuicao(gerarCnj(1850)))).toBe('1900-01-01');
    });
  });

  describe('dataDistribuicaoValidator', () => {
    const HOJE = criarData(2025, 6, 10);

    function validar(data: Date | null, numeroCnj = '0001234-71.2024.8.26.0100') {
      const form = new FormGroup({
        numeroCnj: new FormControl(numeroCnj),
        dataDistribuicao: new FormControl<Date | null>(data, dataDistribuicaoValidator(() => HOJE)),
      });

      form.controls.dataDistribuicao.updateValueAndValidity();
      return form.controls.dataDistribuicao.errors;
    }

    it('aceita do início do ano de ajuizamento até hoje', () => {
      expect(validar(criarData(2024, 1, 1))).toBeNull();
      expect(validar(HOJE)).toBeNull();
    });

    it('rejeita data futura', () => {
      expect(validar(criarData(2025, 6, 11))).toEqual({ dataFutura: true });
    });

    it('rejeita data anterior ao ano de ajuizamento, informando o ano', () => {
      expect(validar(criarData(2023, 12, 31))).toEqual({ dataAnteriorAjuizamento: { ano: 2024 } });
    });

    it('rejeita ano absurdo (ex.: 0004) pelo piso de 1900', () => {
      expect(validar(criarData(4, 4, 11))).toEqual({ dataAnteriorMinima: true });
    });

    it('sem CNJ válido aplica só o piso e o limite de hoje', () => {
      expect(validar(criarData(1990, 5, 5), '')).toBeNull();
      expect(validar(criarData(1899, 12, 31), '')).toEqual({ dataAnteriorMinima: true });
    });

    it('ignora vazio e data inválida (tratados por required e pelo datepicker)', () => {
      expect(validar(null)).toBeNull();
      expect(validar(new Date(Number.NaN))).toBeNull();
    });

    it('funciona fora de um formulário e usa a data de hoje por padrão', () => {
      expect(dataDistribuicaoValidator()(new FormControl(hoje()))).toBeNull();
    });
  });

  describe('valorMonetarioValidator', () => {
    const validar = (valor: number | null) => valorMonetarioValidator(1000)(new FormControl(valor));

    it.each([0, 10, 10.5, 10.55, 1000])('aceita %s', (valor) => {
      expect(validar(valor)).toBeNull();
    });

    it('rejeita negativo, acima do máximo e mais de 2 casas', () => {
      expect(validar(-0.01)).toEqual({ valorNegativo: true });
      expect(validar(1000.01)).toEqual({ valorMaximo: true });
      expect(validar(10.555)).toEqual({ casasDecimais: true });
    });

    it('ignora vazio e NaN (obrigatoriedade é do Validators.required)', () => {
      expect(validar(null)).toBeNull();
      expect(validar(Number.NaN)).toBeNull();
    });
  });
});
