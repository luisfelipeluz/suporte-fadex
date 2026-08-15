import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { chamados as apiChamados, dashboard } from '../api/servicos';
import type { ChamadoResumo, Metricas, Prioridade, StatusChamado } from '../api/tipos';
import { useAuth } from '../auth/AuthContext';
import { OrigemBadge, PriorityBadge, StatusBadge } from '../components/Badges';
import { EstadoErro, EstadoVazio, EsqueletoLinhas } from '../components/Estados';
import { Icone } from '../components/Icone';
import { QuadroKanban } from '../components/QuadroKanban';
import { useRealtime } from '../realtime/RealtimeContext';
import { dataRelativa, percentual } from '../utils/formato';

const CORES_STATUS: Record<StatusChamado, string> = {
  ABERTO: 'var(--ac)',
  EM_ANDAMENTO: 'var(--md)',
  RESOLVIDO: 'var(--lo)',
  FECHADO: 'var(--dim)',
  CANCELADO: 'var(--dim)',
};

const CORES_PRIORIDADE: Record<Prioridade, string> = {
  ALTA: 'var(--hi)',
  MEDIA: 'var(--md)',
  BAIXA: 'var(--lo)',
};

const ORDEM_STATUS: StatusChamado[] = ['ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO'];
const ORDEM_PRIORIDADE: Prioridade[] = ['ALTA', 'MEDIA', 'BAIXA'];

export function Dashboard() {
  const { admin } = useAuth();
  // As métricas chegam pelo SSE; a chamada REST serve como estado inicial e
  // como caminho de recuperação caso o canal ainda não tenha conectado.
  const { metricas: metricasTempoReal, revisao, conexao } = useRealtime();
  const navegar = useNavigate();

  const [metricasIniciais, setMetricasIniciais] = useState<Metricas | null>(null);
  const [recentes, setRecentes] = useState<ChamadoResumo[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<unknown>(null);
  const [kanbanAberto, setKanbanAberto] = useState(false);

  const metricas = metricasTempoReal ?? metricasIniciais;

  function carregar() {
    setCarregando(true);
    setErro(null);

    Promise.all([dashboard.metricas(), apiChamados.listar({ size: 6 })])
      .then(([m, pagina]) => {
        setMetricasIniciais(m);
        setRecentes(pagina.conteudo);
      })
      .catch(setErro)
      .finally(() => setCarregando(false));
  }

  useEffect(carregar, []);

  // Recarrega a lista de recentes a cada evento de chamado.
  useEffect(() => {
    if (revisao === 0) return;
    apiChamados
      .listar({ size: 6 })
      .then((pagina) => setRecentes(pagina.conteudo))
      .catch(() => {
        /* o estado anterior continua válido */
      });
  }, [revisao]);

  if (erro && !metricas) {
    return (
      <div className="card">
        <EstadoErro erro={erro} aoTentarNovamente={carregar} />
      </div>
    );
  }

  const total = metricas?.total ?? 0;

  const kpis = [
    { rotulo: 'Total de chamados', valor: total, cor: 'var(--ac)', destaque: false },
    { rotulo: 'Abertos', valor: metricas?.porStatus.ABERTO ?? 0, cor: 'var(--ac)', destaque: false },
    {
      rotulo: 'Em andamento',
      valor: metricas?.porStatus.EM_ANDAMENTO ?? 0,
      cor: 'var(--md)',
      destaque: false,
    },
    {
      rotulo: 'Resolvidos',
      valor: metricas?.porStatus.RESOLVIDO ?? 0,
      cor: 'var(--lo)',
      destaque: false,
    },
    {
      rotulo: 'Alta prioridade',
      valor: metricas?.altaPrioridadeEmAberto ?? 0,
      cor: 'var(--hi)',
      destaque: true,
    },
  ];

  const maiorStatus = Math.max(
    1,
    ...ORDEM_STATUS.map((s) => metricas?.porStatus[s] ?? 0),
  );

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      {/* Cabeçalho */}
      <div
        style={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 26, fontWeight: 800, letterSpacing: '-0.025em' }}>
            Visão geral
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: 14, color: 'var(--mut)' }}>
            {admin
              ? 'Acompanhe os chamados e a operação do suporte em tempo real.'
              : 'Acompanhe os seus chamados e as respostas do suporte.'}
          </p>
        </div>

        {/* Afastado do título, no canto oposto do cabeçalho. */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginLeft: 'auto' }}>
          <span
            className="badge"
            style={{
              background: conexao === 'conectado' ? 'var(--lol)' : 'var(--mdl)',
              color: conexao === 'conectado' ? 'var(--lof)' : 'var(--mdf)',
            }}
          >
            <span
              aria-hidden="true"
              style={{
                width: 7,
                height: 7,
                borderRadius: '50%',
                background: conexao === 'conectado' ? 'var(--lo)' : 'var(--md)',
              }}
            />
            {conexao === 'conectado' ? 'Atualização em tempo real' : 'Reconectando…'}
          </span>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={() => setKanbanAberto(true)}
            title="Abrir o quadro Kanban do fluxo de chamados"
          >
            <Icone nome="colunas" tamanho={15} traco={1.9} /> Kanban
          </button>
        </div>
      </div>

      {/* KPIs */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
          gap: 12,
        }}
      >
        {kpis.map((kpi) => (
          <div
            key={kpi.rotulo}
            className="card"
            style={{
              padding: 16,
              borderColor: kpi.destaque && kpi.valor > 0 ? 'var(--hib)' : 'var(--bd)',
            }}
          >
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                gap: 8,
              }}
            >
              <span
                style={{
                  fontSize: 11.5,
                  fontWeight: 700,
                  letterSpacing: '0.05em',
                  textTransform: 'uppercase',
                  color: 'var(--mut)',
                }}
              >
                {kpi.rotulo}
              </span>
              <span
                aria-hidden="true"
                style={{ width: 8, height: 8, borderRadius: '50%', background: kpi.cor }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, margin: '9px 0 0' }}>
              <span
                style={{
                  fontSize: 30,
                  fontWeight: 800,
                  lineHeight: 1,
                  letterSpacing: '-0.03em',
                  fontVariantNumeric: 'tabular-nums',
                  color: kpi.destaque && kpi.valor > 0 ? 'var(--hif)' : 'var(--ink)',
                }}
              >
                {kpi.valor}
              </span>
              <span
                style={{
                  fontSize: 11.5,
                  fontWeight: 600,
                  color: kpi.destaque && kpi.valor > 0 ? 'var(--hif)' : 'var(--mut)',
                }}
              >
                {kpi.destaque
                  ? kpi.valor > 0
                    ? 'requer atenção'
                    : 'nada pendente'
                  : `${percentual(kpi.valor, total)}% do total`}
              </span>
            </div>

            {/* A barra repete em forma o que o texto acima diz em numero: a
                fatia deste status dentro do total. */}
            <div
              aria-hidden="true"
              style={{
                height: 3,
                marginTop: 9,
                background: 'var(--ntl)',
                borderRadius: 3,
                overflow: 'hidden',
              }}
            >
              <span
                style={{
                  display: 'block',
                  height: '100%',
                  width: `${percentual(kpi.valor, total)}%`,
                  background: kpi.cor,
                  animation: 'bar .6s ease',
                }}
              />
            </div>
          </div>
        ))}
      </div>

      {/* Distribuições */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))',
          gap: 16,
        }}
      >
        <section className="card card-pad">
          <h2 style={{ margin: '0 0 16px', fontSize: 15 }}>Chamados por status</h2>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {ORDEM_STATUS.map((status) => {
              const valor = metricas?.porStatus[status] ?? 0;
              return (
                <div key={status}>
                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      fontSize: 13,
                      marginBottom: 5,
                    }}
                  >
                    <span style={{ color: 'var(--ink2)' }}>
                      {status.replace('_', ' ').toLowerCase()}
                    </span>
                    <span style={{ fontWeight: 700 }}>{valor}</span>
                  </div>
                  <div style={{ height: 7, background: 'var(--ntl)', borderRadius: 999 }}>
                    <div
                      style={{
                        height: '100%',
                        width: `${(valor / maiorStatus) * 100}%`,
                        background: CORES_STATUS[status],
                        borderRadius: 999,
                        animation: 'bar .5s ease',
                      }}
                    />
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="card card-pad">
          <h2 style={{ margin: '0 0 16px', fontSize: 15 }}>Chamados por prioridade</h2>
          <div style={{ display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}>
            <div
              aria-hidden="true"
              style={{
                width: 110,
                height: 110,
                borderRadius: '50%',
                flexShrink: 0,
                background: `conic-gradient(${CORES_PRIORIDADE.ALTA} 0% ${percentual(
                  metricas?.porPrioridade.ALTA ?? 0,
                  total,
                )}%, ${CORES_PRIORIDADE.MEDIA} ${percentual(
                  metricas?.porPrioridade.ALTA ?? 0,
                  total,
                )}% ${percentual(
                  (metricas?.porPrioridade.ALTA ?? 0) + (metricas?.porPrioridade.MEDIA ?? 0),
                  total,
                )}%, ${CORES_PRIORIDADE.BAIXA} ${percentual(
                  (metricas?.porPrioridade.ALTA ?? 0) + (metricas?.porPrioridade.MEDIA ?? 0),
                  total,
                )}% 100%)`,
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <span
                style={{
                  width: 72,
                  height: 72,
                  borderRadius: '50%',
                  background: 'var(--sf)',
                  display: 'grid',
                  placeItems: 'center',
                  fontWeight: 800,
                  fontSize: 18,
                }}
              >
                {total}
              </span>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, flex: 1, minWidth: 140 }}>
              {ORDEM_PRIORIDADE.map((p) => {
                const valor = metricas?.porPrioridade[p] ?? 0;
                return (
                  <div key={p} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span
                      aria-hidden="true"
                      style={{
                        width: 9,
                        height: 9,
                        borderRadius: 2,
                        background: CORES_PRIORIDADE[p],
                      }}
                    />
                    <PriorityBadge prioridade={p} />
                    <span style={{ marginLeft: 'auto', fontWeight: 700, fontSize: 14 }}>{valor}</span>
                    <span style={{ fontSize: 12, color: 'var(--mut)', width: 40, textAlign: 'right' }}>
                      {percentual(valor, total)}%
                    </span>
                  </div>
                );
              })}
            </div>
          </div>

          <p
            style={{
              margin: '16px 0 0',
              paddingTop: 12,
              borderTop: '1px solid var(--bd)',
              fontSize: 13,
              color: 'var(--mut)',
            }}
          >
            <strong style={{ color: 'var(--acd)' }}>
              {metricas?.percentualClassificadoPorIa ?? 0}%
            </strong>{' '}
            dos chamados foram classificados automaticamente pela IA
            {metricas ? ` · ${metricas.porOrigem.MANUAL} ajustados manualmente` : ''}.
          </p>
        </section>
      </div>

      {/* Recentes */}
      <section className="card">
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--bd)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <h2 style={{ margin: 0, fontSize: 15 }}>Chamados recentes</h2>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => navegar('/chamados')}
          >
            Ver todos
          </button>
        </div>

        {carregando && recentes.length === 0 ? (
          <EsqueletoLinhas linhas={5} />
        ) : recentes.length === 0 ? (
          <EstadoVazio
            titulo="Nenhum chamado ainda"
            descricao="Assim que o primeiro chamado for aberto, ele aparece aqui."
            acao={
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => navegar('/chamados/novo')}
              >
                Abrir chamado
              </button>
            }
          />
        ) : (
          <div className="tabela-wrap">
            <table className="tabela">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Título</th>
                  <th>Categoria</th>
                  <th>Prioridade</th>
                  <th>Status</th>
                  <th>Solicitante</th>
                  <th>Responsável</th>
                  <th>Classificação</th>
                  <th>Criado em</th>
                </tr>
              </thead>
              <tbody>
                {recentes.map((c) => (
                  <tr key={c.id} onClick={() => navegar(`/chamados/${c.id}`)}>
                    <td className="mono" style={{ color: 'var(--mut)' }}>
                      #{c.id}
                    </td>
                    <td style={{ fontWeight: 600, color: 'var(--ink)' }}>{c.titulo}</td>
                    <td>{c.categoriaRotulo}</td>
                    <td>
                      <PriorityBadge prioridade={c.prioridade} />
                    </td>
                    <td>
                      <StatusBadge status={c.status} />
                    </td>
                    <td>{c.solicitante.nome}</td>
                    <td style={{ color: c.responsavel ? 'var(--ink2)' : 'var(--dim)' }}>
                      {c.responsavel?.nome ?? 'Não atribuído'}
                    </td>
                    <td>
                      <OrigemBadge origem={c.origemClassificacao} />
                    </td>
                    <td style={{ whiteSpace: 'nowrap' }}>{dataRelativa(c.criadoEm)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <QuadroKanban aberto={kanbanAberto} aoFechar={() => setKanbanAberto(false)} />
    </div>
  );
}
