import { HttpInterceptorFn } from '@angular/common/http';

export const CORRELATION_ID_HEADER = 'X-Correlation-Id';

export const correlationIdInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/') || req.headers.has(CORRELATION_ID_HEADER)) {
    return next(req);
  }
  return next(req.clone({ setHeaders: { [CORRELATION_ID_HEADER]: crypto.randomUUID() } }));
};
