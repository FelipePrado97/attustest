import { aplicarMascara, posicaoAposDigitos, somenteDigitos } from './mascara';

describe('máscara numérica', () => {
  const DATA = '00/00/0000';

  it.each([
    ['', ''],
    ['1', '1'],
    ['15', '15'],
    ['150', '15/0'],
    ['1503', '15/03'],
    ['15032', '15/03/2'],
    ['15032024', '15/03/2024'],
  ])('aplica o padrão progressivamente: "%s" -> "%s"', (entrada, esperado) => {
    expect(aplicarMascara(entrada, DATA)).toBe(esperado);
  });

  it('descarta letras e símbolos, aceitando só dígitos', () => {
    expect(aplicarMascara('1a5/b03-2024x', DATA)).toBe('15/03/2024');
    expect(aplicarMascara('abc', DATA)).toBe('');
  });

  it('descarta dígitos além do padrão', () => {
    expect(aplicarMascara('150320249999', DATA)).toBe('15/03/2024');
  });

  it('reaplicar sobre um valor já formatado não o altera', () => {
    expect(aplicarMascara('15/03/2024', DATA)).toBe('15/03/2024');
  });

  it('não deixa separador "pendurado" ao apagar', () => {
    expect(aplicarMascara('15/', DATA)).toBe('15');
  });

  it('posicaoAposDigitos encontra a posição do cursor no texto formatado', () => {
    expect(posicaoAposDigitos('15/03/2024', 0)).toBe(0);
    expect(posicaoAposDigitos('15/03/2024', 2)).toBe(2);
    expect(posicaoAposDigitos('15/03/2024', 3)).toBe(4);
    expect(posicaoAposDigitos('15/03/2024', 8)).toBe(10);
    expect(posicaoAposDigitos('15/03', 9)).toBe(5);
  });

  it('somenteDigitos remove tudo que não é número', () => {
    expect(somenteDigitos('0001234-71.2024.8.26.0100')).toBe('00012347120248260100');
  });
});
