-- =============================================================================
-- V1 — Schema inicial da Central de Chamados
-- =============================================================================
-- O DDL e deliberadamente portavel (sem clausulas ENGINE/CHARSET especificas do
-- MySQL) para que estas mesmas migrations rodem tambem em H2 na suite de testes,
-- garantindo que o schema testado seja exatamente o schema de producao.
-- O charset utf8mb4 e definido na criacao do banco (docker-compose), nao por tabela.
-- =============================================================================

CREATE TABLE usuario (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    papel         VARCHAR(20)  NOT NULL,
    criado_em     DATETIME(6)  NOT NULL,
    atualizado_em DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
);

CREATE TABLE chamado (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    titulo                 VARCHAR(200)  NOT NULL,
    descricao              VARCHAR(4000) NOT NULL,

    -- classificacao final vigente
    categoria              VARCHAR(20)   NOT NULL,
    prioridade             VARCHAR(10)   NOT NULL,
    origem_classificacao   VARCHAR(10)   NOT NULL,

    -- sugestao original da triagem automatica (preservada para auditoria)
    categoria_sugerida     VARCHAR(20),
    prioridade_sugerida    VARCHAR(10),
    confianca_ia           VARCHAR(10),
    justificativa_ia       VARCHAR(4000),
    provedor_triagem       VARCHAR(60),
    classificacao_revisada BOOLEAN       NOT NULL DEFAULT FALSE,

    -- ciclo de vida
    status                 VARCHAR(20)   NOT NULL,
    solicitante_id         BIGINT        NOT NULL,
    responsavel_id         BIGINT,
    criado_em              DATETIME(6)   NOT NULL,
    atualizado_em          DATETIME(6)   NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_chamado_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuario (id),
    CONSTRAINT fk_chamado_responsavel FOREIGN KEY (responsavel_id) REFERENCES usuario (id)
);

-- Indices que sustentam os filtros da listagem e as consultas do dashboard.
CREATE INDEX idx_chamado_status       ON chamado (status);
CREATE INDEX idx_chamado_prioridade   ON chamado (prioridade);
CREATE INDEX idx_chamado_categoria    ON chamado (categoria);
CREATE INDEX idx_chamado_solicitante  ON chamado (solicitante_id);
CREATE INDEX idx_chamado_responsavel  ON chamado (responsavel_id);
CREATE INDEX idx_chamado_criado_em    ON chamado (criado_em);

CREATE TABLE comentario (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    chamado_id BIGINT        NOT NULL,
    autor_id   BIGINT        NOT NULL,
    texto      VARCHAR(2000) NOT NULL,
    criado_em  DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comentario_chamado FOREIGN KEY (chamado_id) REFERENCES chamado (id),
    CONSTRAINT fk_comentario_autor   FOREIGN KEY (autor_id)   REFERENCES usuario (id)
);

CREATE INDEX idx_comentario_chamado ON comentario (chamado_id, criado_em);

CREATE TABLE evento_historico (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    chamado_id BIGINT       NOT NULL,
    autor_id   BIGINT,
    tipo       VARCHAR(40)  NOT NULL,
    descricao  VARCHAR(500) NOT NULL,
    etiqueta   VARCHAR(60),
    criado_em  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_evento_chamado FOREIGN KEY (chamado_id) REFERENCES chamado (id),
    CONSTRAINT fk_evento_autor   FOREIGN KEY (autor_id)   REFERENCES usuario (id)
);

CREATE INDEX idx_evento_chamado ON evento_historico (chamado_id, criado_em);
