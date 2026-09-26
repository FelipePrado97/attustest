const MARCADOR_DE_DIGITO = '0';

export function somenteDigitos(valor: string): string {
  return valor.replace(/\D/g, '');
}

export function aplicarMascara(valor: string, padrao: string): string {
  const digitos = somenteDigitos(valor);
  let resultado = '';
  let proximo = 0;
  for (const simbolo of padrao) {
    if (proximo >= digitos.length) {
      break;
    }
    if (simbolo === MARCADOR_DE_DIGITO) {
      resultado += digitos[proximo++];
    } else {
      resultado += simbolo;
    }
  }
  return resultado;
}

export function posicaoAposDigitos(texto: string, quantidade: number): number {
  if (quantidade <= 0) {
    return 0;
  }
  let vistos = 0;
  for (let i = 0; i < texto.length; i++) {
    if (/\d/.test(texto[i]) && ++vistos === quantidade) {
      return i + 1;
    }
  }
  return texto.length;
}
