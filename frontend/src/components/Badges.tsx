/**
 * Badges semânticos de status, prioridade e origem da classificação.
 *
 * As cores seguem exatamente os tokens do design. O rótulo textual acompanha
 * sempre a cor: quem não distingue as cores continua lendo a informação.
 */

import type { OrigemClassificacao, Prioridade, StatusChamado } from '../api/tipos';
import { ICONE_STATUS, ROTULO_PRIORIDADE, ROTULO_STATUS } from '../utils/formato';

const CORES_PRIORIDADE: Record<Prioridade, { bg: string; fg: string; ponto: string }> = {
  ALTA: { bg: 'var(--hil)', fg: '#b3261e', ponto: '#dc2626' },
  MEDIA: { bg: 'var(--mdl)', fg: '#8a4008', ponto: '#b45309' },
  BAIXA: { bg: 'var(--lol)', fg: '#0b5f58', ponto: '#0f766e' },
};

/** Exportado para que o quadro Kanban pinte as colunas com as mesmas cores dos badges. */
export const CORES_STATUS: Record<StatusChamado, { bg: string; fg: string; cor: string }> = {
  ABERTO: { bg: 'var(--acl)', fg: '#1a4fbd', cor: '#2563eb' },
  EM_ANDAMENTO: { bg: 'var(--mdl)', fg: '#8a4008', cor: '#b45309' },
  RESOLVIDO: { bg: 'var(--lol)', fg: '#0b5f58', cor: '#0f766e' },
  FECHADO: { bg: 'var(--ntl)', fg: '#475569', cor: '#94a3b8' },
  CANCELADO: { bg: 'var(--ntl)', fg: '#475569', cor: '#94a3b8' },
};

export function PriorityBadge({ prioridade }: { prioridade: Prioridade }) {
  const cor = CORES_PRIORIDADE[prioridade];
  return (
    <span className="badge" style={{ background: cor.bg, color: cor.fg }}>
      <span
        aria-hidden="true"
        style={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          background: cor.ponto,
          display: 'inline-block',
        }}
      />
      {ROTULO_PRIORIDADE[prioridade]}
    </span>
  );
}

export function StatusBadge({ status }: { status: StatusChamado }) {
  const cor = CORES_STATUS[status];
  return (
    <span className="badge" style={{ background: cor.bg, color: cor.fg }}>
      <span aria-hidden="true">{ICONE_STATUS[status]}</span>
      {ROTULO_STATUS[status]}
    </span>
  );
}

/** Deixa visualmente evidente se a classificação veio da IA ou de uma correção. */
export function OrigemBadge({ origem }: { origem: OrigemClassificacao }) {
  const ia = origem === 'IA';
  return (
    <span
      className="badge"
      style={{
        background: ia ? 'var(--acl)' : 'var(--ntl)',
        color: ia ? 'var(--acd)' : 'var(--ink2)',
        border: `1px solid ${ia ? '#c7d7fb' : 'var(--bd)'}`,
      }}
      title={ia ? 'Classificado automaticamente pela IA' : 'Classificação ajustada manualmente'}
    >
      {ia ? '✨ IA' : 'MANUAL'}
    </span>
  );
}

export function Avatar({
  iniciais,
  destaque = false,
  tamanho = 32,
}: {
  iniciais: string;
  destaque?: boolean;
  tamanho?: number;
}) {
  return (
    <span
      aria-hidden="true"
      style={{
        width: tamanho,
        height: tamanho,
        borderRadius: '50%',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: destaque ? 'var(--acl)' : 'var(--ntl)',
        color: destaque ? 'var(--acd)' : 'var(--ink2)',
        fontSize: tamanho * 0.4,
        fontWeight: 700,
        flexShrink: 0,
      }}
    >
      {iniciais || '—'}
    </span>
  );
}
