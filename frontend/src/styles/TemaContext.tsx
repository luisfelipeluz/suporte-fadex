/**
 * Tema claro/escuro.
 *
 * São três estados, não dois: `claro`, `escuro` e `sistema` — este último
 * segue a preferência do sistema operacional e continua seguindo se ela mudar
 * no meio da sessão. Guardar só um booleano perderia essa terceira opção, que
 * é o padrão de quem nunca mexeu no seletor.
 *
 * A escolha vira `data-theme` no elemento raiz, e é o CSS que decide o resto
 * (`tokens.css`). Nenhum componente pergunta qual é o tema para escolher cor:
 * se precisasse, a cor estaria no lugar errado.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

export type Tema = 'claro' | 'escuro' | 'sistema';

const CHAVE = 'fadex.tema';

interface TemaContextValor {
  /** O que está escolhido, incluindo "sistema". */
  tema: Tema;
  /** O que está valendo na tela agora — "sistema" já resolvido. */
  efetivo: 'claro' | 'escuro';
  definirTema: (t: Tema) => void;
  /** Alterna entre claro e escuro a partir do que está valendo. */
  alternar: () => void;
}

const Ctx = createContext<TemaContextValor | null>(null);

function lerPreferencia(): Tema {
  const salvo = localStorage.getItem(CHAVE);
  return salvo === 'claro' || salvo === 'escuro' ? salvo : 'sistema';
}

function sistemaPrefereEscuro(): boolean {
  return window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
}

export function TemaProvider({ children }: { children: ReactNode }) {
  const [tema, setTema] = useState<Tema>(lerPreferencia);
  const [sistemaEscuro, setSistemaEscuro] = useState(sistemaPrefereEscuro);

  // Acompanha a troca de tema do sistema enquanto a sessão está aberta —
  // sem isso, quem está em "sistema" só veria a mudança ao recarregar.
  useEffect(() => {
    const mq = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!mq) return;

    const aoMudar = (e: MediaQueryListEvent) => setSistemaEscuro(e.matches);
    mq.addEventListener('change', aoMudar);
    return () => mq.removeEventListener('change', aoMudar);
  }, []);

  const efetivo: 'claro' | 'escuro' =
    tema === 'sistema' ? (sistemaEscuro ? 'escuro' : 'claro') : tema;

  useEffect(() => {
    const raiz = document.documentElement;

    if (tema === 'sistema') {
      // Sem o atributo, quem manda é a media query do CSS.
      raiz.removeAttribute('data-theme');
      localStorage.removeItem(CHAVE);
    } else {
      raiz.setAttribute('data-theme', tema === 'escuro' ? 'dark' : 'light');
      localStorage.setItem(CHAVE, tema);
    }
  }, [tema]);

  const definirTema = useCallback((t: Tema) => setTema(t), []);
  const alternar = useCallback(
    () => setTema(efetivo === 'escuro' ? 'claro' : 'escuro'),
    [efetivo],
  );

  const valor = useMemo(
    () => ({ tema, efetivo, definirTema, alternar }),
    [tema, efetivo, definirTema, alternar],
  );

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>;
}

export function useTema() {
  const ctx = useContext(Ctx);
  if (!ctx) throw new Error('useTema precisa estar dentro de <TemaProvider>');
  return ctx;
}
