/** Formatação de datas e rótulos exibidos na interface. */

import type { Confianca, Prioridade, StatusChamado, TipoEvento } from '../api/tipos';

const HOJE_MS = 24 * 60 * 60 * 1000;

/**
 * Data relativa e legível: "Hoje 10:32", "Ontem 16:41" ou "12/08 15:22".
 *
 * A API devolve instantes em UTC (ISO-8601); a conversão para o fuso do usuário
 * acontece aqui, no navegador.
 */
export function dataRelativa(iso: string): string {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '—';

  const hora = data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

  const inicioHoje = new Date();
  inicioHoje.setHours(0, 0, 0, 0);

  const diff = inicioHoje.getTime() - data.getTime();

  if (diff <= 0) return `Hoje ${hora}`;
  if (diff <= HOJE_MS) return `Ontem ${hora}`;

  const dia = data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' });
  return `${dia} ${hora}`;
}

/** Apenas o horário, usado na timeline. */
export function horaDe(iso: string): string {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

export function dataCompleta(iso: string): string {
  const data = new Date(iso);
  if (Number.isNaN(data.getTime())) return '—';
  return data.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

/** Rótulos acentuados, já que os valores da API trafegam em ASCII. */
export const ROTULO_PRIORIDADE: Record<Prioridade, string> = {
  BAIXA: 'BAIXA',
  MEDIA: 'MÉDIA',
  ALTA: 'ALTA',
};

export const ROTULO_STATUS: Record<StatusChamado, string> = {
  ABERTO: 'ABERTO',
  EM_ANDAMENTO: 'EM ANDAMENTO',
  RESOLVIDO: 'RESOLVIDO',
  FECHADO: 'FECHADO',
  CANCELADO: 'CANCELADO',
};

export const ROTULO_CONFIANCA: Record<Confianca, string> = {
  BAIXA: 'Baixa',
  MEDIA: 'Média',
  ALTA: 'Alta',
};

/** Ícone textual de cada status, acompanhando a cor no badge. */
/**
 * Cor do marcador de cada evento no histórico.
 *
 * A API também manda uma cor em `evento.cor`, mas ela é um literal fixo: não
 * acompanharia a troca de tema, e ficaria na paleta antiga. O tipo do evento é
 * o dado que importa — a cor sai dele, aqui, em token.
 */
export const COR_EVENTO: Record<TipoEvento, string> = {
  CHAMADO_ABERTO: 'var(--ac)',
  CLASSIFICACAO_IA: 'var(--ac)',
  CLASSIFICACAO_ACEITA: 'var(--lo)',
  CLASSIFICACAO_CORRIGIDA: 'var(--md)',
  RESPONSAVEL_ATRIBUIDO: 'var(--mut)',
  STATUS_ALTERADO: 'var(--ac)',
  STATUS_RETROCEDIDO: 'var(--md)',
  COMENTARIO_ADICIONADO: 'var(--mut)',
  CHAMADO_ATUALIZADO: 'var(--mut)',
  CHAMADO_CANCELADO: 'var(--dim)',
};

export const ICONE_STATUS: Record<StatusChamado, string> = {
  ABERTO: '○',
  EM_ANDAMENTO: '◐',
  RESOLVIDO: '✓',
  FECHADO: '⊘',
  CANCELADO: '⊘',
};

export function percentual(parte: number, total: number): number {
  return total === 0 ? 0 : Math.round((parte / total) * 100);
}

/** Nome legível de cada mecanismo de triagem. */
const ROTULO_PROVEDOR: Record<string, string> = {
  gemini: 'Gemini',
  heuristic: 'Heurística local',
  // Devolvido quando nem o provider configurado nem a heurística responderam.
  indisponivel: 'Indisponível',
  // Chamados anteriores ao registro do provedor.
  desconhecido: 'Não registrado',
};

/**
 * Rótulo do provider de triagem.
 *
 * A chave vem livre da API — o provider é configurável no backend —, então um
 * nome desconhecido é exibido como veio, em vez de sumir da tela.
 */
export function rotuloProvedor(nome: string): string {
  return ROTULO_PROVEDOR[nome] ?? nome;
}

/** Identifica os providers que representam IA externa de verdade. */
export function ehIaExterna(nome: string): boolean {
  return nome === 'gemini';
}
