export const MASCARA_DATA = '00/00/0000';

export function criarData(ano: number, mes: number, dia: number): Date {
  const data = new Date(2000, 0, 1);
  data.setFullYear(ano, mes - 1, dia);
  return data;
}

export function dataExiste(ano: number, mes: number, dia: number): boolean {
  const data = criarData(ano, mes, dia);
  return data.getFullYear() === ano && data.getMonth() === mes - 1 && data.getDate() === dia;
}

export function isoParaData(iso: string): Date {
  const [ano, mes, dia] = iso.split('-').map(Number);
  return criarData(ano, mes, dia);
}

export function dataParaIso(data: Date): string {
  const ano = String(data.getFullYear()).padStart(4, '0');
  const mes = String(data.getMonth() + 1).padStart(2, '0');
  const dia = String(data.getDate()).padStart(2, '0');
  return `${ano}-${mes}-${dia}`;
}

export function hoje(): Date {
  const agora = new Date();
  return criarData(agora.getFullYear(), agora.getMonth() + 1, agora.getDate());
}
