import { useNavigate } from 'react-router-dom';

import { useRealtime } from '../realtime/RealtimeContext';
import { dataRelativa } from '../utils/formato';

/**
 * Alerta imediato de chamado de prioridade ALTA.
 *
 * Requisito específico do desafio: quando um chamado ALTA é aberto, a equipe
 * precisa ser avisada na hora. Chama atenção sem bloquear a tela — o usuário
 * continua trabalhando e decide quando abrir o chamado.
 */
export function AlertaAltaPrioridade() {
  const { alerta, dispensarAlerta } = useRealtime();
  const navegar = useNavigate();

  if (!alerta) return null;

  return (
    <div
      role="alert"
      className="card"
      style={{
        position: 'fixed',
        top: 74,
        right: 20,
        width: 'min(380px, calc(100vw - 40px))',
        padding: 16,
        borderColor: '#f6c9c9',
        borderLeft: '4px solid var(--hi)',
        boxShadow: 'var(--sh3)',
        animation: 'inR .3s ease, plsr 2s ease 3',
        zIndex: 80,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <span aria-hidden="true" style={{ fontSize: 18, lineHeight: 1.2 }}>
          🚨
        </span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 800, color: '#b3261e' }}>
            Nova solicitação de alta prioridade
          </p>
          <p style={{ margin: '6px 0 0', fontSize: 14, fontWeight: 700 }}>{alerta.titulo}</p>

          <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--mut)' }}>
            #{alerta.id} · {alerta.categoriaRotulo} · {alerta.solicitante.nome}
          </p>
          <p style={{ margin: '2px 0 0', fontSize: 12, color: '#94a3b8' }}>
            {dataRelativa(alerta.criadoEm)}
          </p>

          <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
            <button
              type="button"
              className="btn btn-danger btn-sm"
              onClick={() => {
                const id = alerta.id;
                dispensarAlerta();
                navegar(`/chamados/${id}`);
              }}
            >
              Ver chamado
            </button>
            <button type="button" className="btn btn-secondary btn-sm" onClick={dispensarAlerta}>
              Dispensar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
