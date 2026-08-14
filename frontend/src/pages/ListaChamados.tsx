import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { chamados as apiChamados } from '../api/servicos';
import type {
  Categoria,
  ChamadoResumo,
  Pagina,
  Prioridade,
  StatusChamado,
} from '../api/tipos';
import { useAuth } from '../auth/AuthContext';
import { OrigemBadge, PriorityBadge, StatusBadge } from '../components/Badges';
import { Icone } from '../components/Icone';
import { EstadoErro, EstadoVazio, EsqueletoLinhas } from '../components/Estados';
import { useRealtime } from '../realtime/RealtimeContext';
import { dataRelativa, ROTULO_PRIORIDADE } from '../utils/formato';

const TAMANHO_PAGINA = 10;

const OPCOES_STATUS: StatusChamado[] = [
  'ABERTO',
  'EM_ANDAMENTO',
  'RESOLVIDO',
  'FECHADO',
  'CANCELADO',
];
const OPCOES_PRIORIDADE: Prioridade[] = ['ALTA', 'MEDIA', 'BAIXA'];
const OPCOES_CATEGORIA: Categoria[] = [
  'HARDWARE',
  'SOFTWARE',
  'ACESSO',
  'REDE',
  'SISTEMAS',
  'OUTROS',
];

export function ListaChamados() {
  const { admin } = useAuth();
  const { revisao } = useRealtime();
  const navegar = useNavigate();

  const [status, setStatus] = useState<StatusChamado | ''>('');
  const [prioridade, setPrioridade] = useState<Prioridade | ''>('');
  const [categoria, setCategoria] = useState<Categoria | ''>('');
  const [busca, setBusca] = useState('');
  const [buscaAplicada, setBuscaAplicada] = useState('');
  const [pagina, setPagina] = useState(0);

  const [dados, setDados] = useState<Pagina<ChamadoResumo> | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<unknown>(null);

  // Espera o usuário parar de digitar antes de consultar a API.
  useEffect(() => {
    const timer = setTimeout(() => {
      setBuscaAplicada(busca);
      setPagina(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [busca]);

  const carregar = useCallback(() => {
    setCarregando(true);
    setErro(null);

    apiChamados
      .listar({
        status: status || undefined,
        prioridade: prioridade || undefined,
        categoria: categoria || undefined,
        busca: buscaAplicada || undefined,
        page: pagina,
        size: TAMANHO_PAGINA,
      })
      .then(setDados)
      .catch(setErro)
      .finally(() => setCarregando(false));
  }, [status, prioridade, categoria, buscaAplicada, pagina]);

  useEffect(carregar, [carregar]);

  // Um chamado criado ou alterado em outra sessão recarrega a lista.
  useEffect(() => {
    if (revisao > 0) carregar();
  }, [revisao, carregar]);

  const filtrosAtivos = [
    status && { rotulo: `Status: ${status.replace('_', ' ')}`, limpar: () => setStatus('') },
    prioridade && {
      rotulo: `Prioridade: ${ROTULO_PRIORIDADE[prioridade]}`,
      limpar: () => setPrioridade(''),
    },
    categoria && { rotulo: `Categoria: ${categoria}`, limpar: () => setCategoria('') },
    buscaAplicada && {
      rotulo: `Busca: ${buscaAplicada}`,
      limpar: () => {
        setBusca('');
        setBuscaAplicada('');
      },
    },
  ].filter(Boolean) as { rotulo: string; limpar: () => void }[];

  function limparTudo() {
    setStatus('');
    setPrioridade('');
    setCategoria('');
    setBusca('');
    setBuscaAplicada('');
    setPagina(0);
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          gap: 16,
          flexWrap: 'wrap',
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: 22, letterSpacing: '-0.02em' }}>
            {admin ? 'Chamados' : 'Meus chamados'}
          </h1>
          <p style={{ margin: '4px 0 0', fontSize: 14, color: 'var(--mut)' }}>
            {admin
              ? 'Fila completa do suporte, com filtros por status, prioridade e categoria.'
              : 'Acompanhe os chamados que você abriu e as respostas do suporte.'}
          </p>
        </div>

        <button
          type="button"
          className="btn btn-primary"
          onClick={() => navegar('/chamados/novo')}
        >
          <Icone nome="mais" tamanho={15} traco={2.2} />
          Novo chamado
        </button>
      </div>

      {/* Filtros */}
      <div className="card" style={{ padding: 16 }}>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'minmax(200px, 2fr) repeat(3, minmax(140px, 1fr)) auto',
            gap: 10,
            alignItems: 'end',
          }}
          className="filtros"
        >
          <div className="field">
            <label className="label" htmlFor="busca">
              Buscar
            </label>
            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
              <Icone
                nome="busca"
                tamanho={15}
                cor="#94a3b8"
                style={{ position: 'absolute', left: 11, pointerEvents: 'none' }}
              />
              <input
                id="busca"
                className="input"
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                placeholder="Buscar por título, número ou solicitante"
                style={{ paddingLeft: 34 }}
              />
            </div>
          </div>

          <div className="field">
            <label className="label" htmlFor="f-status">
              Status
            </label>
            <select
              id="f-status"
              className="select"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value as StatusChamado | '');
                setPagina(0);
              }}
            >
              <option value="">Todos</option>
              {OPCOES_STATUS.map((s) => (
                <option key={s} value={s}>
                  {s.replace('_', ' ')}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="label" htmlFor="f-prioridade">
              Prioridade
            </label>
            <select
              id="f-prioridade"
              className="select"
              value={prioridade}
              onChange={(e) => {
                setPrioridade(e.target.value as Prioridade | '');
                setPagina(0);
              }}
            >
              <option value="">Todas</option>
              {OPCOES_PRIORIDADE.map((p) => (
                <option key={p} value={p}>
                  {ROTULO_PRIORIDADE[p]}
                </option>
              ))}
            </select>
          </div>

          <div className="field">
            <label className="label" htmlFor="f-categoria">
              Categoria
            </label>
            <select
              id="f-categoria"
              className="select"
              value={categoria}
              onChange={(e) => {
                setCategoria(e.target.value as Categoria | '');
                setPagina(0);
              }}
            >
              <option value="">Todas</option>
              {OPCOES_CATEGORIA.map((c) => (
                <option key={c} value={c}>
                  {c.charAt(0) + c.slice(1).toLowerCase()}
                </option>
              ))}
            </select>
          </div>

          <button
            type="button"
            className="btn btn-secondary"
            onClick={limparTudo}
            disabled={filtrosAtivos.length === 0}
          >
            Limpar
          </button>
        </div>

        {filtrosAtivos.length > 0 && (
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 12 }}>
            {filtrosAtivos.map((f) => (
              <button
                key={f.rotulo}
                type="button"
                onClick={f.limpar}
                className="badge"
                style={{
                  background: 'var(--acl)',
                  color: 'var(--acd)',
                  border: '1px solid #c7d7fb',
                  cursor: 'pointer',
                }}
              >
                {f.rotulo} <span aria-hidden="true">×</span>
                <span className="sr-only">Remover filtro</span>
              </button>
            ))}
          </div>
        )}
      </div>

      {/* Resultados */}
      <div className="card">
        {carregando && !dados ? (
          <EsqueletoLinhas />
        ) : erro ? (
          <EstadoErro erro={erro} aoTentarNovamente={carregar} />
        ) : !dados || dados.conteudo.length === 0 ? (
          <EstadoVazio
            titulo={
              filtrosAtivos.length > 0 ? 'Nenhum resultado para estes filtros' : 'Nenhum chamado ainda'
            }
            descricao={
              filtrosAtivos.length > 0
                ? 'Tente ajustar ou limpar os filtros aplicados.'
                : 'Quando um chamado for aberto, ele aparece nesta lista.'
            }
            acao={
              filtrosAtivos.length > 0 ? (
                <button type="button" className="btn btn-secondary" onClick={limparTudo}>
                  Limpar filtros
                </button>
              ) : (
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={() => navegar('/chamados/novo')}
                >
                  Abrir chamado
                </button>
              )
            }
          />
        ) : (
          <>
            <div className="tabela-wrap">
              <table className="tabela">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Título</th>
                    <th>Categoria</th>
                    <th>Prioridade</th>
                    <th>Status</th>
                    {admin && <th>Solicitante</th>}
                    <th>Responsável</th>
                    <th>Classificação</th>
                    <th>Atualizado</th>
                  </tr>
                </thead>
                <tbody>
                  {dados.conteudo.map((c) => (
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
                      {admin && <td>{c.solicitante.nome}</td>}
                      <td style={{ color: c.responsavel ? 'var(--ink2)' : '#94a3b8' }}>
                        {c.responsavel?.nome ?? 'Não atribuído'}
                      </td>
                      <td>
                        <OrigemBadge origem={c.origemClassificacao} />
                      </td>
                      <td style={{ whiteSpace: 'nowrap' }}>{dataRelativa(c.atualizadoEm)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Paginação */}
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                padding: '12px 16px',
                gap: 12,
                flexWrap: 'wrap',
              }}
            >
              <span style={{ fontSize: 13, color: 'var(--mut)' }}>
                {dados.totalElementos} chamado{dados.totalElementos === 1 ? '' : 's'} · página{' '}
                {dados.pagina + 1} de {dados.totalPaginas}
              </span>

              <div style={{ display: 'flex', gap: 6 }}>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPagina((p) => Math.max(0, p - 1))}
                  disabled={dados.primeira}
                >
                  Anterior
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setPagina((p) => p + 1)}
                  disabled={dados.ultima}
                >
                  Próxima
                </button>
              </div>
            </div>
          </>
        )}
      </div>

      <style>{`
        @media (max-width: 860px) {
          .filtros { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
