import { rotuloAcaoStatus } from './processo.labels';

describe('rotuloAcaoStatus', () => {
  it.each([
    ['ATIVO', 'SUSPENSO', 'Suspender'],
    ['ATIVO', 'ARQUIVADO', 'Arquivar'],
    ['SUSPENSO', 'ATIVO', 'Reativar'],
    ['ARQUIVADO', 'ATIVO', 'Desarquivar'],
  ] as const)('%s -> %s = %s', (origem, destino, rotulo) => {
    expect(rotuloAcaoStatus(origem, destino)).toBe(rotulo);
  });
});
