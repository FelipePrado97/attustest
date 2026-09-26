CREATE TABLE processo (
    id                 UUID                     NOT NULL,
    numero_cnj         VARCHAR(25)              NOT NULL,
    numero_cnj_digitos VARCHAR(20)              NOT NULL,
    assunto            VARCHAR(200)             NOT NULL,
    parte_contraria    VARCHAR(150)             NOT NULL,
    valor_causa        NUMERIC(15, 2)           NOT NULL,
    data_distribuicao  DATE                     NOT NULL,
    status             VARCHAR(20)              NOT NULL,
    versao             BIGINT                   NOT NULL,
    criado_em          TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_processo PRIMARY KEY (id),
    CONSTRAINT uk_processo_numero_cnj UNIQUE (numero_cnj),
    CONSTRAINT ck_processo_status CHECK (status IN ('ATIVO', 'SUSPENSO', 'ARQUIVADO')),
    CONSTRAINT ck_processo_valor_causa CHECK (valor_causa >= 0)
);

CREATE INDEX ix_processo_status ON processo (status);
CREATE INDEX ix_processo_atualizado_em ON processo (atualizado_em);

COMMENT ON TABLE processo IS 'Processos judiciais acompanhados pela procuradoria (agregado Processo)';

COMMENT ON COLUMN processo.id IS 'Identificador do processo (UUID gerado pela aplicação)';
COMMENT ON COLUMN processo.numero_cnj IS 'Número único do processo no padrão CNJ, formatado (NNNNNNN-DD.AAAA.J.TR.OOOO), com dígito verificador válido; único e imutável';
COMMENT ON COLUMN processo.numero_cnj_digitos IS 'Mesmo número CNJ só com os 20 dígitos, sem máscara; usado na busca por número digitado com ou sem pontuação';
COMMENT ON COLUMN processo.assunto IS 'Assunto ou objeto da ação (até 200 caracteres)';
COMMENT ON COLUMN processo.parte_contraria IS 'Nome da parte contrária no processo (até 150 caracteres)';
COMMENT ON COLUMN processo.valor_causa IS 'Valor da causa em reais: maior ou igual a zero, com até 13 dígitos inteiros e 2 casas decimais';
COMMENT ON COLUMN processo.data_distribuicao IS 'Data de distribuição: a partir de 01/01/1900 e do ano de ajuizamento do número CNJ, e não futura';
COMMENT ON COLUMN processo.status IS 'Situação do processo: ATIVO, SUSPENSO ou ARQUIVADO (processo arquivado não pode ser editado)';
COMMENT ON COLUMN processo.versao IS 'Versão para controle de concorrência otimista (@Version); incrementada a cada alteração';
COMMENT ON COLUMN processo.criado_em IS 'Instante do cadastro (UTC)';
COMMENT ON COLUMN processo.atualizado_em IS 'Instante da última alteração (UTC); ordena a listagem, mais recentes primeiro';
