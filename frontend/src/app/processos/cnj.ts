import { aplicarMascara, somenteDigitos } from '../core/mascara/mascara';

export const MASCARA_CNJ = '0000000-00.0000.0.00.0000';
export const TOTAL_DIGITOS_CNJ = 20;

export function formatarCnj(valor: string): string {
  return aplicarMascara(valor, MASCARA_CNJ);
}

export function cnjValido(valor: string): boolean {
  const d = somenteDigitos(valor);
  if (d.length !== TOTAL_DIGITOS_CNJ) {
    return false;
  }
  const reordenado = d.slice(0, 7) + d.slice(9) + d.slice(7, 9);
  return BigInt(reordenado) % 97n === 1n;
}

export function anoAjuizamento(numeroCnj: string | null | undefined): number | null {
  const digitos = somenteDigitos(numeroCnj ?? '');
  return cnjValido(digitos) ? Number(digitos.slice(9, 13)) : null;
}

export type Aleatorio = (limite: number) => number;

const aleatorioSeguro: Aleatorio = (limite) => crypto.getRandomValues(new Uint32Array(1))[0] % limite;

export function gerarCnj(ano: number = new Date().getFullYear(), aleatorio: Aleatorio = aleatorioSeguro): string {
  const sequencial = String(aleatorio(10_000_000)).padStart(7, '0');
  const origem = String(1 + aleatorio(9_999)).padStart(4, '0');
  const semDigito = `${sequencial}${String(ano).padStart(4, '0')}826${origem}`;
  const digitoVerificador = String(98 - Number(BigInt(semDigito + '00') % 97n)).padStart(2, '0');
  return formatarCnj(sequencial + digitoVerificador + semDigito.slice(7));
}
