CREATE TABLE processo (
    id                UUID           NOT NULL,
    numero_cnj        VARCHAR(25)    NOT NULL,
    numero_cnj_digitos VARCHAR(20)   NOT NULL, -- mesma informação sem máscara, para busca
    assunto           VARCHAR(200)   NOT NULL,
    parte_contraria   VARCHAR(150)   NOT NULL,
    valor_causa       NUMERIC(15, 2) NOT NULL,
    data_distribuicao DATE           NOT NULL,
    status            VARCHAR(20)    NOT NULL,
    versao            BIGINT         NOT NULL,
    criado_em         TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em     TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_processo PRIMARY KEY (id),
    CONSTRAINT uk_processo_numero_cnj UNIQUE (numero_cnj),
    CONSTRAINT ck_processo_status CHECK (status IN ('ATIVO', 'SUSPENSO', 'ARQUIVADO')),
    CONSTRAINT ck_processo_valor_causa CHECK (valor_causa >= 0)
);

CREATE INDEX ix_processo_status ON processo (status);
CREATE INDEX ix_processo_atualizado_em ON processo (atualizado_em);
