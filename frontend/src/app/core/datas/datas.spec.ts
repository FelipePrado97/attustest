import { criarData, dataExiste, dataParaIso, hoje, isoParaData } from './datas';

describe('datas', () => {
  it('isoParaData cria a data local, sem o deslocamento de fuso do new Date(iso)', () => {
    const data = isoParaData('2024-03-15');

    expect([data.getFullYear(), data.getMonth(), data.getDate(), data.getHours()]).toEqual([2024, 2, 15, 0]);
  });

  it('ida e volta preservam a data', () => {
    expect(dataParaIso(isoParaData('2024-12-31'))).toBe('2024-12-31');
  });

  it('preserva anos de 0 a 99 (o construtor do Date os trataria como 19xx)', () => {
    expect(criarData(4, 4, 11).getFullYear()).toBe(4);
    expect(dataParaIso(criarData(4, 4, 11))).toBe('0004-04-11');
  });

  it('dataExiste reconhece datas impossíveis', () => {
    expect(dataExiste(2024, 2, 29)).toBe(true);
    expect(dataExiste(2023, 2, 29)).toBe(false);
    expect(dataExiste(2024, 4, 31)).toBe(false);
    expect(dataExiste(2024, 13, 1)).toBe(false);
  });

  it('hoje é a data local à meia-noite', () => {
    const agora = new Date();
    const data = hoje();

    expect(dataParaIso(data)).toBe(dataParaIso(agora));
    expect(data.getHours()).toBe(0);
  });
});
