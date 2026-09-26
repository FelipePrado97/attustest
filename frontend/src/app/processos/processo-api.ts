import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  DadosProcesso,
  FiltroProcessos,
  NovoProcesso,
  Pagina,
  Processo,
  RegistroHistorico,
  StatusProcesso,
} from './processo.model';

@Injectable({ providedIn: 'root' })
export class ProcessoApi {
  static readonly BASE = '/api/v1/processos';

  private readonly http = inject(HttpClient);

  listar(filtro: FiltroProcessos): Observable<Pagina<Processo>> {
    let params = new HttpParams().set('pagina', filtro.pagina).set('tamanho', filtro.tamanho);
    const termo = filtro.termo?.trim();
    if (termo) {
      params = params.set('termo', termo);
    }
    if (filtro.status) {
      params = params.set('status', filtro.status);
    }
    return this.http.get<Pagina<Processo>>(ProcessoApi.BASE, { params });
  }

  buscar(id: string): Observable<Processo> {
    return this.http.get<Processo>(`${ProcessoApi.BASE}/${id}`);
  }

  cadastrar(processo: NovoProcesso): Observable<Processo> {
    return this.http.post<Processo>(ProcessoApi.BASE, processo);
  }

  atualizar(id: string, versao: number, dados: DadosProcesso): Observable<Processo> {
    return this.http.put<Processo>(`${ProcessoApi.BASE}/${id}`, { ...dados, versao });
  }

  alterarStatus(id: string, versao: number, status: StatusProcesso): Observable<Processo> {
    return this.http.patch<Processo>(`${ProcessoApi.BASE}/${id}/status`, { status, versao });
  }

  excluir(id: string): Observable<void> {
    return this.http.delete<void>(`${ProcessoApi.BASE}/${id}`);
  }

  historico(id: string): Observable<RegistroHistorico[]> {
    return this.http.get<RegistroHistorico[]>(`${ProcessoApi.BASE}/${id}/historico`);
  }
}
