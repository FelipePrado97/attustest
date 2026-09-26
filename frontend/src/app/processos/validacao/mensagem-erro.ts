import { ValidationErrors } from '@angular/forms';

import { LIMITES } from '../processo.model';

type Mensagem = (detalhe: unknown) => string;

const moeda = (valor: number) => valor.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const MENSAGENS: readonly [string, Mensagem][] = [
  ['servidor', (detalhe) => String(detalhe)],
  ['matDatepickerParse', () => 'Data inválida. Use o formato dd/mm/aaaa.'],
  ['required', () => 'Campo obrigatório.'],
  ['maxlength', (detalhe) => `Use no máximo ${(detalhe as { requiredLength: number }).requiredLength} caracteres.`],
  ['cnjIncompleto', () => 'O número CNJ deve ter 20 dígitos.'],
  ['cnjDigitoVerificador', () => 'Número CNJ inválido: dígito verificador não confere.'],
  ['dataAnteriorMinima', () => 'A data não pode ser anterior a 01/01/1900.'],
  [
    'dataAnteriorAjuizamento',
    (detalhe) => `A data não pode ser anterior ao ano de ajuizamento do processo (${(detalhe as { ano: number }).ano}).`,
  ],
  ['dataFutura', () => 'A data de distribuição não pode ser futura.'],
  ['matDatepickerMin', () => 'Data anterior ao mínimo permitido.'],
  ['matDatepickerMax', () => 'A data de distribuição não pode ser futura.'],
  ['valorNegativo', () => 'O valor não pode ser negativo.'],
  ['valorMaximo', () => `O valor máximo é ${moeda(LIMITES.valorCausaMaximo)}.`],
  ['casasDecimais', () => 'Use no máximo 2 casas decimais.'],
];

export function mensagemDeErro(erros: ValidationErrors | null): string {
  if (!erros) {
    return '';
  }
  const aplicavel = MENSAGENS.find(([chave]) => erros[chave]);
  return aplicavel ? aplicavel[1](erros[aplicavel[0]]) : 'Valor inválido.';
}
