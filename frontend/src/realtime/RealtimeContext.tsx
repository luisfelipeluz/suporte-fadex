/**
 * Canal de tempo real (Server-Sent Events).
 *
 * Mantém uma única conexão SSE para toda a aplicação — abrir uma por tela
 * multiplicaria conexões no servidor sem necessidade — e distribui o que chega
 * para quem precisa: KPIs do dashboard, listagem de chamados, central de
 * notificações e o alerta de prioridade ALTA.
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

import { lerToken } from '../api/client';
import type { ChamadoResumo, Metricas } from '../api/tipos';
import { useAuth } from '../auth/AuthContext';
import { useToasts } from '../components/Toasts';

export type EstadoConexao = 'conectando' | 'conectado' | 'reconectando';

export interface Notificacao {
  id: string;
  titulo: string;
  detalhe: string;
  cor: string;
  quando: string;
  chamadoId: number;
  lida: boolean;
}

interface EstadoRealtime {
  conexao: EstadoConexao;
  metricas: Metricas | null;
  notificacoes: Notificacao[];
  naoLidas: number;
  /** Alerta de chamado ALTA aguardando ser visto pela equipe. */
  alerta: ChamadoResumo | null;
  /** Incrementa a cada evento de chamado; listas observam para recarregar. */
  revisao: number;
  dispensarAlerta: () => void;
  marcarTodasLidas: () => void;
}

const RealtimeContext = createContext<EstadoRealtime | null>(null);

const LIMITE_NOTIFICACOES = 20;

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { usuario } = useAuth();
  const { notificar } = useToasts();

  const [conexao, setConexao] = useState<EstadoConexao>('conectando');
  const [metricas, setMetricas] = useState<Metricas | null>(null);
  const [notificacoes, setNotificacoes] = useState<Notificacao[]>([]);
  const [alerta, setAlerta] = useState<ChamadoResumo | null>(null);
  const [revisao, setRevisao] = useState(0);

  // Mantido em ref para que os listeners do EventSource — registrados uma única
  // vez — enxerguem sempre o valor atual sem recriar a conexão a cada render.
  const notificarRef = useRef(notificar);
  notificarRef.current = notificar;

  const adicionarNotificacao = useCallback(
    (titulo: string, detalhe: string, cor: string, chamadoId: number) => {
      setNotificacoes((atuais) =>
        [
          {
            id: Math.random().toString(36).slice(2),
            titulo,
            detalhe,
            cor,
            quando: new Date().toISOString(),
            chamadoId,
            lida: false,
          },
          ...atuais,
        ].slice(0, LIMITE_NOTIFICACOES),
      );
    },
    [],
  );

  useEffect(() => {
    if (!usuario) {
      setConexao('conectando');
      return;
    }

    const token = lerToken();
    if (!token) return;

    // O EventSource nativo não envia cabeçalhos personalizados; por isso o
    // backend aceita o token por query param exclusivamente nesta rota.
    const fonte = new EventSource(`/api/eventos/stream?token=${encodeURIComponent(token)}`);

    const comDados = <T,>(handler: (dados: T) => void) =>
      (evento: MessageEvent) => {
        try {
          handler(JSON.parse(evento.data) as T);
        } catch {
          // Payload inesperado não pode derrubar o canal inteiro.
        }
      };

    fonte.addEventListener(
      'conectado',
      comDados<Metricas>((dados) => {
        setMetricas(dados);
        setConexao('conectado');
      }),
    );

    fonte.addEventListener('metricas', comDados<Metricas>(setMetricas));

    fonte.addEventListener(
      'chamado-criado',
      comDados<ChamadoResumo>((chamado) => {
        setRevisao((n) => n + 1);

        // O alerta de ALTA tem componente próprio; evita anunciar duas vezes.
        if (chamado.prioridade !== 'ALTA') {
          notificarRef.current(
            'Novo chamado criado',
            `#${chamado.id} · ${chamado.categoriaRotulo} · ${chamado.prioridadeRotulo}`,
            'info',
          );
          adicionarNotificacao(
            'Novo chamado criado',
            `#${chamado.id} · ${chamado.titulo}`,
            '#64748b',
            chamado.id,
          );
        }
      }),
    );

    fonte.addEventListener(
      'chamado-atualizado',
      comDados<ChamadoResumo>((chamado) => {
        setRevisao((n) => n + 1);
        adicionarNotificacao(
          'Chamado atualizado',
          `#${chamado.id} · ${chamado.statusRotulo}`,
          '#2563eb',
          chamado.id,
        );
      }),
    );

    fonte.addEventListener(
      'alerta-alta',
      comDados<ChamadoResumo>((chamado) => {
        setRevisao((n) => n + 1);
        setAlerta(chamado);
        notificarRef.current(
          'Chamado de alta prioridade recebido',
          `#${chamado.id} · ${chamado.titulo}`,
          'erro',
        );
        adicionarNotificacao(
          'Chamado de alta prioridade recebido',
          `#${chamado.id} · ${chamado.titulo}`,
          '#dc2626',
          chamado.id,
        );
      }),
    );

    fonte.onopen = () => setConexao('conectado');

    // O EventSource reconecta sozinho; refletimos isso no indicador da interface
    // em vez de tentar reimplementar a lógica de reconexão à mão.
    fonte.onerror = () => setConexao('reconectando');

    return () => fonte.close();
  }, [usuario, adicionarNotificacao]);

  const valor = useMemo<EstadoRealtime>(
    () => ({
      conexao,
      metricas,
      notificacoes,
      naoLidas: notificacoes.filter((n) => !n.lida).length,
      alerta,
      revisao,
      dispensarAlerta: () => setAlerta(null),
      marcarTodasLidas: () =>
        setNotificacoes((atuais) => atuais.map((n) => ({ ...n, lida: true }))),
    }),
    [conexao, metricas, notificacoes, alerta, revisao],
  );

  return <RealtimeContext.Provider value={valor}>{children}</RealtimeContext.Provider>;
}

export function useRealtime(): EstadoRealtime {
  const contexto = useContext(RealtimeContext);
  if (!contexto) {
    throw new Error('useRealtime precisa estar dentro de <RealtimeProvider>.');
  }
  return contexto;
}
