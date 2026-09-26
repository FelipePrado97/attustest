import { FormGroup } from '@angular/forms';

import { ErroCampo } from '../../core/api/api-error';

export function aplicarErrosDoServidor(form: FormGroup, erros: readonly ErroCampo[]): ErroCampo[] {
  const naoAplicados: ErroCampo[] = [];
  for (const erro of erros) {
    const controle = form.get(erro.campo);
    if (controle) {
      controle.setErrors({ ...controle.errors, servidor: erro.mensagem });
      controle.markAsTouched();
    } else {
      naoAplicados.push(erro);
    }
  }
  return naoAplicados;
}
