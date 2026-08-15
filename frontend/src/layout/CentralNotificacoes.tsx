import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Icone } from '../components/Icone';
import { useRealtime } from '../realtime/RealtimeContext';
import { dataRelativa } from '../utils/formato';

/** Sino de notificações, alimentado pelos eventos reais do canal SSE. */
export function CentralNotificacoes() {
  const { notificacoes, naoLidas, marcarTodasLidas } = useRealtime();
  const [aberto, setAberto] = useState(false);
  const navegar = useNavigate();

  return (
    <div style={{ position: 'relative' }}>
      <button
        type="button"
        className="btn btn-ghost btn-sm"
        onClick={() => {
          setAberto((v) => !v);
          if (!aberto) marcarTodasLidas();
        }}
        aria-expanded={aberto}
        aria-label={
          naoLidas > 0 ? `Notificações, ${naoLidas} não lidas` : 'Notificações'
        }
        style={{
          position: 'relative',
          width: 34,
          height: 34,
          padding: 0,
          display: 'grid',
          placeItems: 'center',
          border: '1px solid var(--bd)',
          background: 'var(--sf)',
          borderRadius: 'var(--rd)',
        }}
      >
        <Icone nome="sino" tamanho={16} traco={1.9} cor="var(--ink2)" />
        {naoLidas > 0 && (
          <span
            aria-hidden="true"
            style={{
              position: 'absolute',
              top: -4,
              right: -4,
              minWidth: 16,
              height: 16,
              padding: '0 4px',
              borderRadius: 999,
              background: 'var(--hi)',
              color: '#fff',
              fontSize: 10,
              fontWeight: 700,
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            {naoLidas}
          </span>
        )}
      </button>

      {aberto && (
        <>
          <div onClick={() => setAberto(false)} style={{ position: 'fixed', inset: 0, zIndex: 35 }} />
          <div
            className="card"
            style={{
              position: 'absolute',
              right: 0,
              top: 'calc(100% + 8px)',
              width: 'min(340px, calc(100vw - 32px))',
              maxHeight: 420,
              overflowY: 'auto',
              boxShadow: 'var(--sh2)',
              zIndex: 40,
            }}
          >
            <div
              style={{
                padding: '12px 14px',
                borderBottom: '1px solid var(--bd)',
                position: 'sticky',
                top: 0,
                background: 'var(--sf)',
              }}
            >
              <p style={{ margin: 0, fontSize: 13, fontWeight: 700 }}>Notificações</p>
              <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>
                Atualizadas em tempo real
              </p>
            </div>

            {notificacoes.length === 0 ? (
              <p
                style={{
                  margin: 0,
                  padding: '28px 16px',
                  fontSize: 13,
                  color: 'var(--mut)',
                  textAlign: 'center',
                }}
              >
                Nenhuma notificação ainda. Novos chamados aparecem aqui automaticamente.
              </p>
            ) : (
              notificacoes.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  onClick={() => {
                    setAberto(false);
                    navegar(`/chamados/${n.chamadoId}`);
                  }}
                  style={{
                    display: 'flex',
                    gap: 10,
                    width: '100%',
                    padding: '11px 14px',
                    border: 'none',
                    borderBottom: '1px solid var(--bd)',
                    background: n.lida ? 'transparent' : 'var(--sf2)',
                    cursor: 'pointer',
                    textAlign: 'left',
                  }}
                >
                  <span
                    aria-hidden="true"
                    style={{
                      width: 8,
                      height: 8,
                      borderRadius: '50%',
                      background: n.cor,
                      marginTop: 5,
                      flexShrink: 0,
                    }}
                  />
                  <span style={{ minWidth: 0 }}>
                    <span style={{ display: 'block', fontSize: 13, fontWeight: 600 }}>
                      {n.titulo}
                    </span>
                    <span
                      style={{
                        display: 'block',
                        fontSize: 12,
                        color: 'var(--mut)',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                    >
                      {n.detalhe}
                    </span>
                    <span style={{ display: 'block', fontSize: 11, color: 'var(--dim)', marginTop: 2 }}>
                      {dataRelativa(n.quando)}
                    </span>
                  </span>
                </button>
              ))
            )}
          </div>
        </>
      )}
    </div>
  );
}
