import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'processos' },
  {
    path: 'processos',
    title: 'Processos',
    loadComponent: () => import('./processos/lista/processo-lista').then((m) => m.ProcessoLista),
  },
  {
    path: 'processos/novo',
    title: 'Novo processo',
    loadComponent: () => import('./processos/formulario/processo-formulario').then((m) => m.ProcessoFormulario),
  },
  {
    path: 'processos/:id',
    title: 'Processo',
    loadComponent: () => import('./processos/detalhe/processo-detalhe').then((m) => m.ProcessoDetalhe),
  },
  {
    path: 'processos/:id/editar',
    title: 'Editar processo',
    loadComponent: () => import('./processos/formulario/processo-formulario').then((m) => m.ProcessoFormulario),
  },
  { path: '**', redirectTo: 'processos' },
];
