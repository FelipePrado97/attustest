import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS, MatFormFieldDefaultOptions } from '@angular/material/form-field';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { correlationIdInterceptor } from './core/api/correlation-id.interceptor';
import { provideLocalizacao } from './core/localizacao';

const CAMPOS_COM_DICAS_QUE_EMPURRAM_O_PROXIMO_CAMPO: MatFormFieldDefaultOptions = {
  appearance: 'outline',
  subscriptSizing: 'dynamic',
};

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([correlationIdInterceptor])),
    provideLocalizacao(),
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: CAMPOS_COM_DICAS_QUE_EMPURRAM_O_PROXIMO_CAMPO },
  ],
};
