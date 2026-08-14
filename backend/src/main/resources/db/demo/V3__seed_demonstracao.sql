-- =============================================================================
-- V3 - Carga de demonstracao
-- =============================================================================
-- Chamados de exemplo para que o avaliador abra o painel ja populado, com os
-- indicadores, a timeline e os comentarios preenchidos.
--
-- Este arquivo vive em `db/demo`, uma location Flyway separada que NAO e
-- carregada no perfil de teste: a suite roda sobre uma base limpa, sem depender
-- destes dados de vitrine. Ver `spring.flyway.locations` em application.yml.
--
-- Usuarios e chamados sao referenciados por e-mail e titulo, e nao por id fixo,
-- para nao depender da ordem de auto incremento.
-- =============================================================================

-- #1 Impressora não está funcionando
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Impressora não está funcionando', 'A impressora do 3º andar não imprime desde ontem. Aparece luz vermelha no painel e o setor inteiro está sem imprimir os empenhos do dia.', 'HARDWARE', 'ALTA', 'IA', 'HARDWARE', 'ALTA', 'ALTA', 'Termos de equipamento físico e falha de dispositivo (impressora, imprime) indicam Hardware. Expressões de indisponibilidade ou bloqueio (não imprime) sustentam prioridade ALTA. O impacto relatado atinge múltiplos usuários (setor inteiro), o que eleva a prioridade.', 'heuristic', FALSE, 'ABERTO', (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), NULL, '2026-08-13 13:32:00.000000', '2026-08-13 13:32:00.000000');

-- #2 Erro ao acessar sistema financeiro
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Erro ao acessar sistema financeiro', 'Ao entrar no módulo de pagamentos o sistema retorna erro 500 e derruba a sessão. Nenhum pagamento pode ser lançado.', 'SISTEMAS', 'ALTA', 'IA', 'SISTEMAS', 'ALTA', 'ALTA', 'Falha em sistema interno de gestão (sistema, financeiro, pagamento) indicam Sistemas. Expressões de indisponibilidade ou bloqueio (erro 500) sustentam prioridade ALTA.', 'heuristic', TRUE, 'EM_ANDAMENTO', (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), '2026-08-13 12:48:00.000000', '2026-08-13 13:12:00.000000');

-- #3 Internet indisponível no bloco B
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Internet indisponível no bloco B', 'Sem conexão cabeada e Wi-Fi instável no bloco B desde as 8h. Cerca de 12 pessoas afetadas.', 'REDE', 'ALTA', 'MANUAL', 'REDE', 'MEDIA', 'MEDIA', 'Referências a conectividade e infraestrutura de rede (internet, conexão, cabeada) indicam Rede. Sinais de degradação com contorno possível (instável) sustentam prioridade MÉDIA. O impacto relatado atinge múltiplos usuários (pessoas afetadas), o que eleva a prioridade.', 'heuristic', TRUE, 'EM_ANDAMENTO', (SELECT id FROM usuario WHERE email = 'carlos.dias@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), '2026-08-13 11:15:00.000000', '2026-08-13 12:30:00.000000');

-- #4 Falha no sistema de folha
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Falha no sistema de folha', 'O relatório de folha do mês fecha com divergência de valores em dois centros de custo.', 'SISTEMAS', 'MEDIA', 'IA', 'SISTEMAS', 'MEDIA', 'MEDIA', 'Falha em sistema interno de gestão (sistema, folha, relatório) indicam Sistemas. Sinais de degradação com contorno possível (divergência) sustentam prioridade MÉDIA.', 'heuristic', TRUE, 'EM_ANDAMENTO', (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), '2026-08-12 19:41:00.000000', '2026-08-13 11:02:00.000000');

-- #5 Solicitação de acesso ao e-mail
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Solicitação de acesso ao e-mail', 'Novo colaborador do setor de convênios precisa de caixa de e-mail institucional.', 'ACESSO', 'BAIXA', 'IA', 'ACESSO', 'BAIXA', 'ALTA', 'Pedido de credencial, permissão ou provisionamento de acesso (acesso, e-mail) indicam Acesso. Não há indicativo de urgência ou de bloqueio da operação, o que caracteriza uma solicitação de rotina.', 'heuristic', TRUE, 'RESOLVIDO', (SELECT id FROM usuario WHERE email = 'lucas.prado@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), '2026-08-12 17:20:00.000000', '2026-08-12 18:05:00.000000');

-- #6 Notebook não liga
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Notebook não liga', 'O notebook do setor de projetos não liga nem carregando. LED de energia apagado.', 'HARDWARE', 'ALTA', 'IA', 'HARDWARE', 'ALTA', 'ALTA', 'Termos de equipamento físico e falha de dispositivo (notebook, não liga) indicam Hardware. Expressões de indisponibilidade ou bloqueio (não liga) sustentam prioridade ALTA.', 'heuristic', TRUE, 'RESOLVIDO', (SELECT id FROM usuario WHERE email = 'carlos.dias@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), '2026-08-12 14:05:00.000000', '2026-08-12 20:40:00.000000');

-- #7 Planilha de prestação de contas travando
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Planilha de prestação de contas travando', 'A planilha de prestação de contas congela ao abrir as abas de anexos e precisa ser fechada à força.', 'SOFTWARE', 'MEDIA', 'IA', 'SOFTWARE', 'MEDIA', 'MEDIA', 'Uso de aplicativo ou software de escritório (planilha) indicam Software. Sinais de degradação com contorno possível (travando, congela) sustentam prioridade MÉDIA.', 'heuristic', FALSE, 'ABERTO', (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), NULL, '2026-08-12 13:12:00.000000', '2026-08-12 13:12:00.000000');

-- #8 Trocar teclado do atendimento
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Trocar teclado do atendimento', 'Teclado do balcão de atendimento com três teclas falhando.', 'HARDWARE', 'BAIXA', 'MANUAL', 'HARDWARE', 'BAIXA', 'MEDIA', 'Termos de equipamento físico e falha de dispositivo (teclado) indicam Hardware. Não há indicativo de urgência ou de bloqueio da operação, o que caracteriza uma solicitação de rotina.', 'heuristic', TRUE, 'FECHADO', (SELECT id FROM usuario WHERE email = 'lucas.prado@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), '2026-08-11 18:22:00.000000', '2026-08-11 19:00:00.000000');

-- #9 Reset de senha do portal do pesquisador
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Reset de senha do portal do pesquisador', 'Preciso redefinir a senha do portal do pesquisador, o link de recuperação não chega.', 'ACESSO', 'BAIXA', 'IA', 'ACESSO', 'BAIXA', 'ALTA', 'Pedido de credencial, permissão ou provisionamento de acesso (senha, redefinir) indicam Acesso. Não há indicativo de urgência ou de bloqueio da operação, o que caracteriza uma solicitação de rotina.', 'heuristic', TRUE, 'FECHADO', (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), '2026-08-11 12:15:00.000000', '2026-08-11 12:38:00.000000');

-- #10 VPN cai a cada 10 minutos
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('VPN cai a cada 10 minutos', 'A VPN desconecta a cada 10 minutos ao acessar os sistemas internos de casa.', 'REDE', 'MEDIA', 'IA', 'REDE', 'MEDIA', 'MEDIA', 'Referências a conectividade e infraestrutura de rede (vpn) indicam Rede. Sinais de degradação com contorno possível (cai a cada, desconecta) sustentam prioridade MÉDIA.', 'heuristic', TRUE, 'RESOLVIDO', (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), '2026-08-10 17:03:00.000000', '2026-08-11 11:20:00.000000');

-- #11 Instalar leitor de PDF assinável
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Instalar leitor de PDF assinável', 'Preciso de um leitor de PDF que permita assinatura digital nos termos de convênio.', 'SOFTWARE', 'BAIXA', 'IA', 'SOFTWARE', 'BAIXA', 'ALTA', 'Uso de aplicativo ou software de escritório (instalar, pdf) indicam Software. Não há indicativo de urgência ou de bloqueio da operação, o que caracteriza uma solicitação de rotina.', 'heuristic', TRUE, 'FECHADO', (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), '2026-08-10 13:40:00.000000', '2026-08-10 14:15:00.000000');

-- #12 Monitor com listras na tela
INSERT INTO chamado (titulo, descricao, categoria, prioridade, origem_classificacao,
    categoria_sugerida, prioridade_sugerida, confianca_ia, justificativa_ia, provedor_triagem,
    classificacao_revisada, status, solicitante_id, responsavel_id, criado_em, atualizado_em)
VALUES ('Monitor com listras na tela', 'O monitor do setor de compras mostra listras verticais intermitentes.', 'HARDWARE', 'MEDIA', 'IA', 'HARDWARE', 'MEDIA', 'MEDIA', 'Termos de equipamento físico e falha de dispositivo (monitor, tela) indicam Hardware. Sinais de degradação com contorno possível (intermitente) sustentam prioridade MÉDIA.', 'heuristic', FALSE, 'ABERTO', (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), NULL, '2026-08-10 11:57:00.000000', '2026-08-10 11:57:00.000000');


-- ---------------------------------------------------------------------------
-- Historico (timeline cronologica de cada chamado)
-- ---------------------------------------------------------------------------

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), 'CHAMADO_ABERTO', 'João Pereira abriu o chamado', NULL, '2026-08-13 13:32:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Hardware / ALTA', 'CONFIANÇA ALTA', '2026-08-13 13:32:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), 'CHAMADO_ABERTO', 'Beatriz Rocha abriu o chamado', NULL, '2026-08-13 12:48:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Sistemas / ALTA', 'CONFIANÇA ALTA', '2026-08-13 12:48:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Maria Lima', NULL, '2026-08-13 13:12:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Maria Lima assumiu o chamado', NULL, '2026-08-13 13:12:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Em andamento', NULL, '2026-08-13 13:12:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'carlos.dias@fadex.org.br'), 'CHAMADO_ABERTO', 'Carlos Dias abriu o chamado', NULL, '2026-08-13 11:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Rede / MÉDIA', 'CONFIANÇA MEDIA', '2026-08-13 11:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'CLASSIFICACAO_CORRIGIDA', 'Classificação corrigida para: Rede / ALTA por Rafael Melo', NULL, '2026-08-13 12:30:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Rafael Melo assumiu o chamado', NULL, '2026-08-13 12:30:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Em andamento', NULL, '2026-08-13 12:30:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Falha no sistema de folha'), (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), 'CHAMADO_ABERTO', 'Beatriz Rocha abriu o chamado', NULL, '2026-08-12 19:41:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Falha no sistema de folha'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Sistemas / MÉDIA', 'CONFIANÇA MEDIA', '2026-08-12 19:41:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Falha no sistema de folha'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Maria Lima', NULL, '2026-08-13 11:02:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Falha no sistema de folha'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Maria Lima assumiu o chamado', NULL, '2026-08-13 11:02:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Falha no sistema de folha'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Em andamento', NULL, '2026-08-13 11:02:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Solicitação de acesso ao e-mail'), (SELECT id FROM usuario WHERE email = 'lucas.prado@fadex.org.br'), 'CHAMADO_ABERTO', 'Lucas Prado abriu o chamado', NULL, '2026-08-12 17:20:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Solicitação de acesso ao e-mail'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Acesso / BAIXA', 'CONFIANÇA ALTA', '2026-08-12 17:20:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Solicitação de acesso ao e-mail'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Camila Reis', NULL, '2026-08-12 18:05:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Solicitação de acesso ao e-mail'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Camila Reis assumiu o chamado', NULL, '2026-08-12 18:05:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Solicitação de acesso ao e-mail'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Resolvido', NULL, '2026-08-12 18:05:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Notebook não liga'), (SELECT id FROM usuario WHERE email = 'carlos.dias@fadex.org.br'), 'CHAMADO_ABERTO', 'Carlos Dias abriu o chamado', NULL, '2026-08-12 14:05:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Notebook não liga'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Hardware / ALTA', 'CONFIANÇA ALTA', '2026-08-12 14:05:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Notebook não liga'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Maria Lima', NULL, '2026-08-12 20:40:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Notebook não liga'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Maria Lima assumiu o chamado', NULL, '2026-08-12 20:40:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Notebook não liga'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Resolvido', NULL, '2026-08-12 20:40:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Planilha de prestação de contas travando'), (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), 'CHAMADO_ABERTO', 'Fernanda Alves abriu o chamado', NULL, '2026-08-12 13:12:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Planilha de prestação de contas travando'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Software / MÉDIA', 'CONFIANÇA MEDIA', '2026-08-12 13:12:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Trocar teclado do atendimento'), (SELECT id FROM usuario WHERE email = 'lucas.prado@fadex.org.br'), 'CHAMADO_ABERTO', 'Lucas Prado abriu o chamado', NULL, '2026-08-11 18:22:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Trocar teclado do atendimento'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Hardware / BAIXA', 'CONFIANÇA MEDIA', '2026-08-11 18:22:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Trocar teclado do atendimento'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'CLASSIFICACAO_CORRIGIDA', 'Classificação corrigida para: Hardware / BAIXA por Camila Reis', NULL, '2026-08-11 19:00:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Trocar teclado do atendimento'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Camila Reis assumiu o chamado', NULL, '2026-08-11 19:00:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Trocar teclado do atendimento'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Fechado', NULL, '2026-08-11 19:00:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Reset de senha do portal do pesquisador'), (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), 'CHAMADO_ABERTO', 'Fernanda Alves abriu o chamado', NULL, '2026-08-11 12:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Reset de senha do portal do pesquisador'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Acesso / BAIXA', 'CONFIANÇA ALTA', '2026-08-11 12:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Reset de senha do portal do pesquisador'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Camila Reis', NULL, '2026-08-11 12:38:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Reset de senha do portal do pesquisador'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Camila Reis assumiu o chamado', NULL, '2026-08-11 12:38:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Reset de senha do portal do pesquisador'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Fechado', NULL, '2026-08-11 12:38:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'VPN cai a cada 10 minutos'), (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), 'CHAMADO_ABERTO', 'João Pereira abriu o chamado', NULL, '2026-08-10 17:03:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'VPN cai a cada 10 minutos'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Rede / MÉDIA', 'CONFIANÇA MEDIA', '2026-08-10 17:03:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'VPN cai a cada 10 minutos'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Rafael Melo', NULL, '2026-08-11 11:20:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'VPN cai a cada 10 minutos'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Rafael Melo assumiu o chamado', NULL, '2026-08-11 11:20:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'VPN cai a cada 10 minutos'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Resolvido', NULL, '2026-08-11 11:20:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Instalar leitor de PDF assinável'), (SELECT id FROM usuario WHERE email = 'beatriz.rocha@fadex.org.br'), 'CHAMADO_ABERTO', 'Beatriz Rocha abriu o chamado', NULL, '2026-08-10 13:40:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Instalar leitor de PDF assinável'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Software / BAIXA', 'CONFIANÇA ALTA', '2026-08-10 13:40:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Instalar leitor de PDF assinável'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'CLASSIFICACAO_ACEITA', 'Classificação da IA aceita por Camila Reis', NULL, '2026-08-10 14:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Instalar leitor de PDF assinável'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'RESPONSAVEL_ATRIBUIDO', 'Camila Reis assumiu o chamado', NULL, '2026-08-10 14:15:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Instalar leitor de PDF assinável'), (SELECT id FROM usuario WHERE email = 'camila.reis@fadex.org.br'), 'STATUS_ALTERADO', 'Status alterado para: Fechado', NULL, '2026-08-10 14:15:00.000000');

INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Monitor com listras na tela'), (SELECT id FROM usuario WHERE email = 'fernanda.alves@fadex.org.br'), 'CHAMADO_ABERTO', 'Fernanda Alves abriu o chamado', NULL, '2026-08-10 11:57:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Monitor com listras na tela'), NULL, 'CLASSIFICACAO_IA', 'IA classificou como: Hardware / MÉDIA', 'CONFIANÇA MEDIA', '2026-08-10 11:57:00.000000');

-- ---------------------------------------------------------------------------
-- Comentarios de exemplo
-- ---------------------------------------------------------------------------

INSERT INTO comentario (chamado_id, autor_id, texto, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), 'Reiniciei a impressora e o problema continua. O setor está sem imprimir os empenhos.', '2026-08-13 13:38:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), (SELECT id FROM usuario WHERE email = 'joao.pereira@fadex.org.br'), 'COMENTARIO_ADICIONADO', 'João Pereira comentou no chamado', NULL, '2026-08-13 13:38:00.000000');

INSERT INTO comentario (chamado_id, autor_id, texto, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'Recebido. Vou verificar o fusor ainda hoje pela manhã e retorno com um prazo.', '2026-08-13 13:44:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Impressora não está funcionando'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'COMENTARIO_ADICIONADO', 'Maria Lima comentou no chamado', NULL, '2026-08-13 13:44:00.000000');

INSERT INTO comentario (chamado_id, autor_id, texto, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'Time de sistemas já replicou o erro em homologação. Correção prevista para hoje.', '2026-08-13 13:12:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Erro ao acessar sistema financeiro'), (SELECT id FROM usuario WHERE email = 'maria.lima@fadex.org.br'), 'COMENTARIO_ADICIONADO', 'Maria Lima comentou no chamado', NULL, '2026-08-13 13:12:00.000000');

INSERT INTO comentario (chamado_id, autor_id, texto, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'Switch do bloco B reiniciado. Acompanhando a estabilidade da conexão pelas próximas horas.', '2026-08-13 12:30:00.000000');
INSERT INTO evento_historico (chamado_id, autor_id, tipo, descricao, etiqueta, criado_em)
VALUES ((SELECT id FROM chamado WHERE titulo = 'Internet indisponível no bloco B'), (SELECT id FROM usuario WHERE email = 'rafael.melo@fadex.org.br'), 'COMENTARIO_ADICIONADO', 'Rafael Melo comentou no chamado', NULL, '2026-08-13 12:30:00.000000');

