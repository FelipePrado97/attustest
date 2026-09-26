import { anoAjuizamento, cnjValido, formatarCnj, gerarCnj } from './cnj';

describe('número CNJ', () => {
  describe('formatarCnj', () => {
    it.each([
      ['', ''],
      ['0001234', '0001234'],
      ['00012347', '0001234-7'],
      ['000123471', '0001234-71'],
      ['0001234712024', '0001234-71.2024'],
      ['00012347120248', '0001234-71.2024.8'],
      ['0001234712024826', '0001234-71.2024.8.26'],
      ['00012347120248260100', '0001234-71.2024.8.26.0100'],
    ])('aplica a máscara progressivamente: %s -> %s', (entrada, esperado) => {
      expect(formatarCnj(entrada)).toBe(esperado);
    });

    it('ignora caracteres não numéricos e o excesso de dígitos', () => {
      expect(formatarCnj('0001234-71.2024.8.26.0100999')).toBe('0001234-71.2024.8.26.0100');
      expect(formatarCnj('abc0001234')).toBe('0001234');
    });

    it('reformatar um valor já formatado não o altera', () => {
      expect(formatarCnj('0001234-71.2024.8.26.0100')).toBe('0001234-71.2024.8.26.0100');
    });
  });

  describe('cnjValido', () => {
    it.each(['0001234-71.2024.8.26.0100', '5001234-17.2023.4.04.7100', '1000000-27.2025.5.02.0001', '00012347120248260100'])(
      'aceita número com dígito verificador correto: %s',
      (numero) => {
        expect(cnjValido(numero)).toBe(true);
      },
    );

    it('rejeita dígito verificador errado e tamanho incorreto', () => {
      expect(cnjValido('0001234-72.2024.8.26.0100')).toBe(false);
      expect(cnjValido('0001234-71.2024')).toBe(false);
    });
  });

  describe('anoAjuizamento', () => {
    it('lê o ano (AAAA) de um CNJ válido, com ou sem máscara', () => {
      expect(anoAjuizamento('0001234-71.2024.8.26.0100')).toBe(2024);
      expect(anoAjuizamento('50012341720234047100')).toBe(2023);
    });

    it('não lê ano de CNJ incompleto ou inválido', () => {
      expect(anoAjuizamento('0001234-71.2024')).toBeNull();
      expect(anoAjuizamento('0001234-72.2024.8.26.0100')).toBeNull();
      expect(anoAjuizamento(null)).toBeNull();
    });
  });

  describe('gerarCnj', () => {
    it('gera número formatado, válido e do ano corrente por padrão', () => {
      const numero = gerarCnj();

      expect(numero).toMatch(/^\d{7}-\d{2}\.\d{4}\.8\.26\.\d{4}$/);
      expect(cnjValido(numero)).toBe(true);
      expect(anoAjuizamento(numero)).toBe(new Date().getFullYear());
    });

    it('usa o ano informado', () => {
      expect(anoAjuizamento(gerarCnj(1998))).toBe(1998);
    });

    it('é determinístico dada a fonte aleatória (sequencial e origem vêm dela)', () => {
      const valores = [1234, 99];
      const numero = gerarCnj(2024, () => valores.shift() ?? 0);

      expect(numero).toBe('0001234-71.2024.8.26.0100');
    });

    it('os extremos da fonte aleatória continuam gerando números válidos', () => {
      expect(cnjValido(gerarCnj(2024, () => 0))).toBe(true);
      expect(cnjValido(gerarCnj(2024, (limite) => limite - 1))).toBe(true);
    });

    it('números gerados em sequência são diferentes', () => {
      const numeros = new Set(Array.from({ length: 50 }, () => gerarCnj()));
      expect(numeros.size).toBe(50);
    });
  });
});
