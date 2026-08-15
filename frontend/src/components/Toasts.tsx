/**
 * Notificações transitórias (toasts).
 *
 * Provider próprio para que qualquer tela possa avisar o usuário sobre o
 * resultado de uma ação sem precisar carregar estado de notificação por conta.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

export type TomToast = 'info' | 'sucesso' | 'alerta' | 'erro';

interface Toast {
  id: string;
  titulo: string;
  detalhe?: string;
  tom: TomToast;
}

const CORES: Record<TomToast, string> = {
  info: 'var(--ac)',
  sucesso: 'var(--lo)',
  alerta: 'var(--md)',
  erro: 'var(--hi)',
};

const DURACAO_MS = 4200;

interface ContextoToast {
  notificar: (titulo: string, detalhe?: string, tom?: TomToast) => void;
}

const ToastContext = createContext<ContextoToast | null>(null);

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);
  const temporizadores = useRef<number[]>([]);

  useEffect(() => {
    const atuais = temporizadores.current;
    return () => atuais.forEach(clearTimeout);
  }, []);

  const notificar = useCallback((titulo: string, detalhe?: string, tom: TomToast = 'info') => {
    const id = Math.random().toString(36).slice(2);
    setToasts((atuais) => [...atuais, { id, titulo, detalhe, tom }]);

    const timer = window.setTimeout(
      () => setToasts((atuais) => atuais.filter((t) => t.id !== id)),
      DURACAO_MS,
    );
    temporizadores.current.push(timer);
  }, []);

  const valor = useMemo(() => ({ notificar }), [notificar]);

  return (
    <ToastContext.Provider value={valor}>
      {children}
      <div
        // `polite` para não interromper o que o leitor de tela está anunciando.
        aria-live="polite"
        style={{
          position: 'fixed',
          right: 20,
          bottom: 20,
          display: 'flex',
          flexDirection: 'column',
          gap: 10,
          zIndex: 90,
          maxWidth: 'min(360px, calc(100vw - 40px))',
        }}
      >
        {toasts.map((t) => (
          <div
            key={t.id}
            className="card"
            style={{
              padding: '12px 14px',
              boxShadow: 'var(--sh2)',
              borderLeft: `3px solid ${CORES[t.tom]}`,
              animation: 'inR .25s ease',
            }}
          >
            <p style={{ margin: 0, fontSize: 14, fontWeight: 700 }}>{t.titulo}</p>
            {t.detalhe && (
              <p style={{ margin: '2px 0 0', fontSize: 13, color: 'var(--mut)' }}>{t.detalhe}</p>
            )}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToasts(): ContextoToast {
  const contexto = useContext(ToastContext);
  if (!contexto) {
    throw new Error('useToasts precisa estar dentro de <ToastProvider>.');
  }
  return contexto;
}
