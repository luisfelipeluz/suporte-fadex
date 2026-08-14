/** Formatação de datas e rótulos exibidos na interface. */

import type { Confianca, Prioridade, StatusChamado } from '../api/tipos';

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
