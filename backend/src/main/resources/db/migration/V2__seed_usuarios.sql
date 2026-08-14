-- =============================================================================
-- V2 — Usuarios de teste
-- =============================================================================
-- O desafio pede credenciais ja cadastradas via seed/migration para agilizar a
-- avaliacao. Todos os usuarios abaixo usam a mesma senha de demonstracao,
-- documentada no README:
--
--     senha: suporte123
--
-- O valor gravado e o hash BCrypt dessa senha — a senha em texto puro nao existe
-- em lugar nenhum do repositorio nem do banco. Estas contas existem apenas para
-- facilitar a avaliacao do desafio.
--
-- Os nomes reproduzem os usuarios do prototipo de interface, de modo que o
-- dashboard ja abra povoado e coerente com o design aprovado.
-- =============================================================================

-- Equipe de suporte (ADMIN) — tambem sao os possiveis responsaveis por chamados.
INSERT INTO usuario (nome, email, senha_hash, papel, criado_em, atualizado_em) VALUES
    ('Ana Souza',    'ana.souza@fadex.org.br',    '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'ADMIN', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Maria Lima',   'maria.lima@fadex.org.br',   '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'ADMIN', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Rafael Melo',  'rafael.melo@fadex.org.br',  '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'ADMIN', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Camila Reis',  'camila.reis@fadex.org.br',  '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'ADMIN', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000');

-- Usuarios internos (SOLICITANTE).
INSERT INTO usuario (nome, email, senha_hash, papel, criado_em, atualizado_em) VALUES
    ('João Pereira',    'joao.pereira@fadex.org.br',    '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'SOLICITANTE', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Beatriz Rocha',   'beatriz.rocha@fadex.org.br',   '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'SOLICITANTE', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Carlos Dias',     'carlos.dias@fadex.org.br',     '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'SOLICITANTE', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Lucas Prado',     'lucas.prado@fadex.org.br',     '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'SOLICITANTE', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
    ('Fernanda Alves',  'fernanda.alves@fadex.org.br',  '$2a$10$5RrwF72uxVlDtaqlvIYCC.QhgtDom4Ej7m2RJc5zKuTvOJgDA4FVG', 'SOLICITANTE', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000');
