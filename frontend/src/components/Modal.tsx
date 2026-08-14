import { useEffect, type ReactNode } from 'react';

/**
 * Diálogo de confirmação.
 *
 * Ações destrutivas nunca são executadas direto: o cancelamento de um chamado
 * passa por aqui. Fecha com Esc e devolve o foco ao fluxo normal da página.
 */
export function Modal({
  aberto,
  titulo,
  descricao,
  rotuloConfirmar,
  tomConfirmar = 'primary',
  processando = false,
  aoConfirmar,
  aoFechar,
  children,
}: {
  aberto: boolean;
  titulo: string;
  descricao?: string;
  rotuloConfirmar: string;
  tomConfirmar?: 'primary' | 'danger';
  processando?: boolean;
  aoConfirmar: () => void;
  aoFechar: () => void;
  children?: ReactNode;
}) {
  useEffect(() => {
    if (!aberto) return;

    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === 'Escape') aoFechar();
    };
    document.addEventListener('keydown', aoTeclar);
    return () => document.removeEventListener('keydown', aoTeclar);
  }, [aberto, aoFechar]);

  if (!aberto) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label={titulo}
      onClick={aoFechar}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(15,23,42,.45)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: 20,
        zIndex: 100,
      }}
    >
      <div
        className="card"
        onClick={(e) => e.stopPropagation()}
        style={{
          maxWidth: 440,
          width: '100%',
          padding: 24,
          boxShadow: 'var(--sh3)',
          animation: 'inU .2s ease',
        }}
      >
        <h2 style={{ margin: '0 0 8px', fontSize: 18 }}>{titulo}</h2>
        {descricao && (
          <p style={{ margin: '0 0 16px', fontSize: 14, color: 'var(--mut)', lineHeight: 1.55 }}>
            {descricao}
          </p>
        )}

        {children}

        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 20 }}>
          <button type="button" className="btn btn-secondary" onClick={aoFechar} disabled={processando}>
            Voltar
          </button>
          <button
            type="button"
            className={`btn btn-${tomConfirmar}`}
            onClick={aoConfirmar}
            disabled={processando}
          >
            {processando ? 'Processando…' : rotuloConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
}
