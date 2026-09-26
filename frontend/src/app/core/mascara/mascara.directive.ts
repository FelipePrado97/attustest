import { DestroyRef, Directive, ElementRef, inject, input } from '@angular/core';

import { aplicarMascara, posicaoAposDigitos, somenteDigitos } from './mascara';

const ANTES_DOS_DEMAIS_LISTENERS: AddEventListenerOptions = { capture: true };

@Directive({ selector: 'input[appMascara]' })
export class MascaraDirective {
  readonly appMascara = input.required<string>();

  constructor() {
    const campo = inject<ElementRef<HTMLInputElement>>(ElementRef).nativeElement;
    const aoDigitar = () => this.formatar(campo);
    campo.addEventListener('input', aoDigitar, ANTES_DOS_DEMAIS_LISTENERS);
    inject(DestroyRef).onDestroy(() => campo.removeEventListener('input', aoDigitar, ANTES_DOS_DEMAIS_LISTENERS));
  }

  private formatar(campo: HTMLInputElement): void {
    const original = campo.value;
    const formatado = aplicarMascara(original, this.appMascara());
    if (formatado === original) {
      return;
    }
    const cursor = campo.selectionStart ?? original.length;
    const digitosAntesDoCursor = somenteDigitos(original.slice(0, cursor)).length;
    campo.value = formatado;
    const novaPosicao = posicaoAposDigitos(formatado, digitosAntesDoCursor);
    campo.setSelectionRange(novaPosicao, novaPosicao);
  }
}
