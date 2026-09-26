import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CORRELATION_ID_HEADER, correlationIdInterceptor } from './correlation-id.interceptor';

describe('correlationIdInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([correlationIdInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('adiciona um UUID novo em cada chamada à API', () => {
    http.get('/api/v1/processos').subscribe();
    http.get('/api/v1/processos').subscribe();

    const [primeira, segunda] = backend.match('/api/v1/processos');
    const id1 = primeira.request.headers.get(CORRELATION_ID_HEADER);
    const id2 = segunda.request.headers.get(CORRELATION_ID_HEADER);
    expect(id1).toMatch(/^[0-9a-f-]{36}$/);
    expect(id1).not.toBe(id2);
    primeira.flush({});
    segunda.flush({});
  });

  it('preserva um id já informado', () => {
    http.get('/api/v1/processos', { headers: { [CORRELATION_ID_HEADER]: 'meu-id-123' } }).subscribe();

    expect(backend.expectOne('/api/v1/processos').request.headers.get(CORRELATION_ID_HEADER)).toBe('meu-id-123');
  });

  it('não envia o header para fora da API', () => {
    http.get('/assets/config.json').subscribe();

    expect(backend.expectOne('/assets/config.json').request.headers.has(CORRELATION_ID_HEADER)).toBe(false);
  });
});
