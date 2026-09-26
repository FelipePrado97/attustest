import { mensagemDeErro } from './mensagem-erro';

describe('mensagemDeErro', () => {
  it('sem erros devolve vazio', () => {
    expect(mensagemDeErro(null)).toBe('');
  });

  it('erro do servidor tem prioridade sobre os demais', () => {
    expect(mensagemDeErro({ required: true, servidor: 'Já existe processo com este número' })).toBe(
      'Já existe processo com este número',
    );
  });

  it.each([
    [{ required: true }, 'Campo obrigatório.'],
    [{ maxlength: { requiredLength: 200, actualLength: 201 } }, 'Use no máximo 200 caracteres.'],
    [{ cnjIncompleto: true }, 'O número CNJ deve ter 20 dígitos.'],
    [{ cnjDigitoVerificador: true }, 'Número CNJ inválido: dígito verificador não confere.'],
    [{ dataFutura: true }, 'A data de distribuição não pode ser futura.'],
    [{ dataAnteriorMinima: true }, 'A data não pode ser anterior a 01/01/1900.'],
    [{ dataAnteriorAjuizamento: { ano: 2024 } }, 'A data não pode ser anterior ao ano de ajuizamento do processo (2024).'],
    [{ matDatepickerMin: {} }, 'Data anterior ao mínimo permitido.'],
    [{ matDatepickerMax: {} }, 'A data de distribuição não pode ser futura.'],
    [{ matDatepickerParse: { text: '31/02' }, required: true }, 'Data inválida. Use o formato dd/mm/aaaa.'],
    [{ valorNegativo: true }, 'O valor não pode ser negativo.'],
    [{ casasDecimais: true }, 'Use no máximo 2 casas decimais.'],
    [{ desconhecido: true }, 'Valor inválido.'],
  ])('%o -> %s', (erros, esperado) => {
    expect(mensagemDeErro(erros)).toBe(esperado);
  });

  it('informa o valor máximo formatado em reais', () => {
    expect(mensagemDeErro({ valorMaximo: true })).toMatch(/^O valor máximo é R\$\s9\.999\.999\.999\.999,99\.$/);
  });
});
