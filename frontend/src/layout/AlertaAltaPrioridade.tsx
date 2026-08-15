import { useNavigate } from 'react-router-dom';

import { Icone } from '../components/Icone';
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
        borderColor: 'var(--hib)',
        borderLeft: '4px solid var(--hi)',
        boxShadow: 'var(--sh3)',
        animation: 'inR .3s ease, plsr 2s ease 3',
        zIndex: 80,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 10 }}>
        <span
          aria-hidden="true"
          style={{
            width: 24,
            height: 24,
            borderRadius: '50%',
            background: 'var(--hi)',
            display: 'grid',
            placeItems: 'center',
            flex: 'none',
            animation: 'plsr 1.6s infinite',
          }}
        >
          <Icone nome="alerta" tamanho={13} traco={2.4} cor="#fff" />
        </span>
        <div style={{ flex: 1, minWidth: 0 }}>
          <p style={{ margin: 0, fontSize: 13, fontWeight: 800, color: 'var(--hif)' }}>
            Nova solicitação de alta prioridade
          </p>
          <p style={{ margin: '6px 0 0', fontSize: 14, fontWeight: 700 }}>{alerta.titulo}</p>

          <p style={{ margin: '4px 0 0', fontSize: 12, color: 'var(--mut)' }}>
            #{alerta.id} · {alerta.categoriaRotulo} · {alerta.solicitante.nome}
          </p>
          <p style={{ margin: '2px 0 0', fontSize: 12, color: 'var(--dim)' }}>
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
