import { ChangeDetectionStrategy, Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { MascaraDirective } from './mascara.directive';

@Component({
  imports: [ReactiveFormsModule, MascaraDirective],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<input appMascara="00/00/0000" [formControl]="campo" />`,
})
class Hospedeiro {
  readonly campo = new FormControl('');
}

describe('MascaraDirective', () => {
  let input: HTMLInputElement;
  let hospedeiro: Hospedeiro;

  beforeEach(async () => {
    const fixture = TestBed.createComponent(Hospedeiro);
    hospedeiro = fixture.componentInstance;
    await fixture.whenStable();
    input = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
  });

  function digitar(valor: string, cursor = valor.length): void {
    input.value = valor;
    input.setSelectionRange(cursor, cursor);
    input.dispatchEvent(new Event('input'));
  }

  it('formata enquanto digita e o formulário recebe o valor já formatado', () => {
    digitar('15032024');

    expect(input.value).toBe('15/03/2024');
    expect(hospedeiro.campo.value).toBe('15/03/2024');
  });

  it('ignora letras: nem a tela nem o formulário as recebem', () => {
    digitar('15/03/2024a');

    expect(input.value).toBe('15/03/2024');
    expect(hospedeiro.campo.value).toBe('15/03/2024');
  });

  it('colar texto com lixo mantém só os dígitos', () => {
    digitar('data: 15-03-2024!');

    expect(input.value).toBe('15/03/2024');
  });

  it('mantém o cursor no lugar ao editar no meio do texto', () => {
    digitar('15/0/2024', 4);

    expect(input.value).toBe('15/02/024');
    expect(input.selectionStart).toBe(4);
  });

  it('não mexe no campo quando o texto já está formatado', () => {
    digitar('15/03', 2);

    expect(input.value).toBe('15/03');
    expect(input.selectionStart).toBe(2);
  });
});
