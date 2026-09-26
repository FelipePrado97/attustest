import { FormControl, FormGroup, Validators } from '@angular/forms';

import { aplicarErrosDoServidor } from './erros-servidor';

describe('aplicarErrosDoServidor', () => {
  const criarForm = () =>
    new FormGroup({
      numeroCnj: new FormControl('0001234-71.2024.8.26.0100', Validators.required),
      assunto: new FormControl('Assunto'),
    });

  it('marca o erro no controle correspondente e o deixa tocado', () => {
    const form = criarForm();

    const naoAplicados = aplicarErrosDoServidor(form, [{ campo: 'numeroCnj', mensagem: 'Número duplicado' }]);

    expect(naoAplicados).toEqual([]);
    expect(form.controls.numeroCnj.errors).toEqual({ servidor: 'Número duplicado' });
    expect(form.controls.numeroCnj.touched).toBe(true);
  });

  it('devolve os erros sem controle correspondente', () => {
    const naoAplicados = aplicarErrosDoServidor(criarForm(), [{ campo: 'versao', mensagem: 'Obrigatório' }]);

    expect(naoAplicados).toEqual([{ campo: 'versao', mensagem: 'Obrigatório' }]);
  });

  it('o erro some quando o usuário altera o valor', () => {
    const form = criarForm();
    aplicarErrosDoServidor(form, [{ campo: 'assunto', mensagem: 'Inválido' }]);

    form.controls.assunto.setValue('Outro assunto');

    expect(form.controls.assunto.errors).toBeNull();
  });
});
