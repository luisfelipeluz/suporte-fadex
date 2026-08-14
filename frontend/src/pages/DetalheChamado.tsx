import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ErroApi } from '../api/erros';
import { chamados, comentarios as apiComentarios, referencias } from '../api/servicos';
import type {
  Categoria,
  ChamadoDetalhe as Chamado,
  Prioridade,
  StatusChamado,
  Usuario,
} from '../api/tipos';
import { useAuth } from '../auth/AuthContext';
import { Avatar, OrigemBadge, PriorityBadge, StatusBadge } from '../components/Badges';
import {
  EstadoCarregando,
  EstadoErro,
  EstadoNaoEncontrado,
  EstadoSemPermissao,
} from '../components/Estados';
import { Modal } from '../components/Modal';
import { useToasts } from '../components/Toasts';
import {
  dataCompleta,
  horaDe,
  ROTULO_CONFIANCA,
  ROTULO_PRIORIDADE,
  ROTULO_STATUS,
} from '../utils/formato';

const FLUXO: StatusChamado[] = ['ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO'];

const CATEGORIAS: Categoria[] = [
  'HARDWARE',
  'SOFTWARE',
  'ACESSO',
  'REDE',
  'SISTEMAS',
  'OUTROS',
];
const PRIORIDADES: Prioridade[] = ['ALTA', 'MEDIA', 'BAIXA'];

export function DetalheChamado() {
  const { id } = useParams();
  const chamadoId = Number(id);
  const { admin, usuario } = useAuth();
  const { notificar } = useToasts();
  const navegar = useNavigate();

  const [chamado, setChamado] = useState<Chamado | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<unknown>(null);
  const [processando, setProcessando] = useState(false);

  const [rascunho, setRascunho] = useState('');
  const [corrigindo, setCorrigindo] = useState(false);
  const [corrCategoria, setCorrCategoria] = useState<Categoria>('HARDWARE');
  const [corrPrioridade, setCorrPrioridade] = useState<Prioridade>('ALTA');
  const [atribuindo, setAtribuindo] = useState(false);
  const [responsaveis, setResponsaveis] = useState<Usuario[]>([]);
  const [confirmandoCancelamento, setConfirmandoCancelamento] = useState(false);

  const carregar = useCallback(() => {
    setCarregando(true);
    setErro(null);

    chamados
      .detalhar(chamadoId)
      .then(setChamado)
      .catch(setErro)
      .finally(() => setCarregando(false));
  }, [chamadoId]);

  useEffect(carregar, [carregar]);

  // A lista de responsáveis é exclusiva do ADMIN.
  useEffect(() => {
    if (!admin) return;
    referencias.responsaveis().then(setResponsaveis).catch(() => setResponsaveis([]));
  }, [admin]);

  /** Envolve uma ação da API tratando erro, aviso e atualização do estado. */
  async function executar(acao: () => Promise<Chamado | void>, sucesso: string, detalhe?: string) {
    setProcessando(true);
    try {
      const atualizado = await acao();
      if (atualizado) {
        setChamado(atualizado);
      } else {
        carregar();
      }
      notificar(sucesso, detalhe, 'sucesso');
    } catch (e) {
      const mensagem =
        e instanceof ErroApi ? e.mensagem : 'Não foi possível concluir a operação.';
      notificar('Ação não permitida', mensagem, 'erro');
    } finally {
      setProcessando(false);
    }
  }

  // ------------------------------------------------------------- estados
  if (carregando && !chamado) return <EstadoCarregando texto="Carregando chamado…" />;

  if (erro instanceof ErroApi && erro.semPermissao) {
    return (
      <div className="card">
        <EstadoSemPermissao />
      </div>
    );
  }

  if (erro instanceof ErroApi && erro.naoEncontrado) {
    return (
      <div className="card">
        <EstadoNaoEncontrado />
      </div>
    );
  }

  if (erro || !chamado) {
    return (
      <div className="card">
        <EstadoErro erro={erro} aoTentarNovamente={carregar} />
      </div>
    );
  }

  const t = chamado.triagem;
  const indiceAtual = FLUXO.indexOf(chamado.status);
  const podeGerenciar = admin && !chamado.encerrado;
  const ehSolicitante = chamado.solicitante.id === usuario?.id;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <button
        type="button"
        className="btn btn-ghost btn-sm"
        onClick={() => navegar('/chamados')}
        style={{ alignSelf: 'flex-start', paddingLeft: 0 }}
      >
        ← Voltar para {admin ? 'chamados' : 'meus chamados'}
      </button>

      {/* ---------------------------------------------------------------- */}
      {/* Cabeçalho                                                         */}
      {/* ---------------------------------------------------------------- */}
      <div className="card card-pad">
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            gap: 16,
            flexWrap: 'wrap',
          }}
        >
          <div style={{ minWidth: 0 }}>
            <span className="mono" style={{ fontSize: 13, color: 'var(--mut)' }}>
              #{chamado.id}
            </span>
            <h1 style={{ margin: '4px 0 10px', fontSize: 21, letterSpacing: '-0.02em' }}>
              {chamado.titulo}
            </h1>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              <StatusBadge status={chamado.status} />
              <PriorityBadge prioridade={chamado.prioridade} />
              <span className="badge" style={{ background: 'var(--ntl)', color: 'var(--ink2)' }}>
                {chamado.categoriaRotulo}
              </span>
              <OrigemBadge origem={t.origem} />
            </div>
          </div>

          {(podeGerenciar || (ehSolicitante && !chamado.encerrado)) && (
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              onClick={() => setConfirmandoCancelamento(true)}
              disabled={processando}
              style={{ alignSelf: 'flex-start' }}
            >
              Cancelar chamado
            </button>
          )}
        </div>

        {/* Informações */}
        <dl
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))',
            gap: 16,
            margin: '20px 0 0',
            paddingTop: 16,
            borderTop: '1px solid var(--bd)',
          }}
        >
          {[
            { rotulo: 'Solicitante', valor: chamado.solicitante.nome },
            { rotulo: 'Responsável', valor: chamado.responsavel?.nome ?? 'Não atribuído' },
            { rotulo: 'Criado em', valor: dataCompleta(chamado.criadoEm) },
            { rotulo: 'Atualizado em', valor: dataCompleta(chamado.atualizadoEm) },
          ].map((item) => (
            <div key={item.rotulo}>
              <dt style={{ fontSize: 12, color: 'var(--mut)' }}>{item.rotulo}</dt>
              <dd style={{ margin: '3px 0 0', fontSize: 14, fontWeight: 600 }}>{item.valor}</dd>
            </div>
          ))}
        </dl>

        {/* Descrição */}
        <div style={{ marginTop: 20, paddingTop: 16, borderTop: '1px solid var(--bd)' }}>
          <h2 style={{ margin: '0 0 8px', fontSize: 13, color: 'var(--mut)' }}>DESCRIÇÃO</h2>
          <p style={{ margin: 0, fontSize: 14, lineHeight: 1.65, whiteSpace: 'pre-wrap' }}>
            {chamado.descricao}
          </p>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'minmax(0, 1.6fr) minmax(0, 1fr)',
          gap: 16,
          alignItems: 'start',
        }}
        className="detalhe-grid"
      >
        {/* -------------------------------------------------------------- */}
        {/* Coluna principal                                                */}
        {/* -------------------------------------------------------------- */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Possiveis duplicados ------------------------------------------ */}
          {chamado.possiveisDuplicados.length > 0 && (
            <section
              className="card card-pad"
              style={{ borderColor: 'var(--md)', background: 'var(--mdl)' }}
              aria-label="Possíveis chamados duplicados"
            >
              <h2 style={{ margin: '0 0 4px', fontSize: 15, color: 'var(--md)' }}>
                ⧉ Possíveis duplicados
              </h2>
              <p style={{ margin: '0 0 14px', fontSize: 13, color: 'var(--ink2)' }}>
                {chamado.possiveisDuplicados.length === 1
                  ? 'Outro chamado usa praticamente o mesmo texto. '
                  : `Outros ${chamado.possiveisDuplicados.length} chamados usam praticamente o mesmo texto. `}
                Verifique antes de abrir uma nova frente de trabalho para o mesmo incidente.
              </p>

              <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: 8 }}>
                {chamado.possiveisDuplicados.map((similar) => (
                  <li key={similar.id}>
                    <button
                      type="button"
                      onClick={() => navegar(`/chamados/${similar.id}`)}
                      style={{
                        width: '100%',
                        textAlign: 'left',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 12,
                        padding: '10px 12px',
                        border: '1px solid var(--bd)',
                        borderRadius: 'var(--rd)',
                        background: 'var(--sf)',
                        cursor: 'pointer',
                        font: 'inherit',
                        color: 'inherit',
                      }}
                    >
                      <span
                        className="badge"
                        style={{ background: 'var(--mdl)', color: 'var(--md)', flexShrink: 0 }}
                        title="Similaridade textual com este chamado"
                      >
                        {similar.similaridade}%
                      </span>

                      <span style={{ minWidth: 0, flex: 1 }}>
                        <span
                          style={{
                            display: 'block',
                            fontSize: 14,
                            fontWeight: 600,
                            overflow: 'hidden',
                            textOverflow: 'ellipsis',
                            whiteSpace: 'nowrap',
                          }}
                        >
                          #{similar.id} — {similar.titulo}
                        </span>
                        <span style={{ display: 'block', fontSize: 12, color: 'var(--mut)' }}>
                          {similar.solicitante.nome} · {dataCompleta(similar.criadoEm)}
                          {similar.termosEmComum.length > 0 && (
                            <> · termos em comum: {similar.termosEmComum.join(', ')}</>
                          )}
                        </span>
                      </span>

                      <span
                        className="badge"
                        style={{
                          background: 'var(--ntl)',
                          color: similar.statusCor,
                          flexShrink: 0,
                        }}
                      >
                        {similar.statusRotulo}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </section>
          )}

          {/* Fluxo de status */}
          <section className="card card-pad">
            <h2 style={{ margin: '0 0 14px', fontSize: 15 }}>Fluxo do chamado</h2>

            <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexWrap: 'wrap' }}>
              {FLUXO.map((s, i) => {
                const concluido = indiceAtual > i;
                const atual = indiceAtual === i;
                return (
                  <div key={s} style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span
                      className="badge"
                      style={{
                        border: `1px solid ${concluido ? '#b9e0d6' : atual ? '#c7d7fb' : 'var(--bd)'}`,
                        background: concluido ? 'var(--lol)' : atual ? 'var(--acl)' : 'transparent',
                        color: concluido ? '#0b5f58' : atual ? 'var(--acd)' : '#94a3b8',
                      }}
                    >
                      <span aria-hidden="true">{concluido ? '✓' : i + 1}</span>
                      {ROTULO_STATUS[s]}
                    </span>
                    {i < FLUXO.length - 1 && (
                      <span aria-hidden="true" style={{ color: '#cbd5e1' }}>
                        →
                      </span>
                    )}
                  </div>
                );
              })}
            </div>

            {chamado.encerrado ? (
              <p
                style={{
                  margin: '14px 0 0',
                  padding: '10px 12px',
                  background: 'var(--ntl)',
                  borderRadius: 8,
                  fontSize: 13,
                  color: 'var(--ink2)',
                }}
              >
                {chamado.status === 'FECHADO'
                  ? 'Chamados fechados não podem ser reabertos.'
                  : 'Este chamado foi cancelado e não admite novas alterações de status.'}
              </p>
            ) : (
              admin &&
              (chamado.proximoStatus || chamado.statusAnterior) && (
                <div
                  style={{
                    marginTop: 14,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 8,
                    flexWrap: 'wrap',
                  }}
                >
                  {/* O retorno vem primeiro por ser o caminho menos usado: fica
                      à vista, mas o avanço continua sendo a ação em destaque. */}
                  {chamado.statusAnterior && (
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      disabled={processando}
                      title={`Devolver o chamado para ${ROTULO_STATUS[chamado.statusAnterior]}`}
                      onClick={() =>
                        executar(
                          () => chamados.alterarStatus(chamado.id, chamado.statusAnterior!),
                          'Chamado retornado',
                          `#${chamado.id} · ${ROTULO_STATUS[chamado.statusAnterior!]}`,
                        )
                      }
                    >
                      <span aria-hidden="true">←</span> Retornar para{' '}
                      {ROTULO_STATUS[chamado.statusAnterior]}
                    </button>
                  )}

                  {chamado.proximoStatus && (
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={processando}
                      onClick={() =>
                        executar(
                          () => chamados.alterarStatus(chamado.id, chamado.proximoStatus!),
                          'Chamado atualizado',
                          `#${chamado.id} · ${ROTULO_STATUS[chamado.proximoStatus!]}`,
                        )
                      }
                    >
                      Mover para {ROTULO_STATUS[chamado.proximoStatus]}
                    </button>
                  )}
                </div>
              )
            )}

            {!admin && !chamado.encerrado && (
              <p style={{ margin: '14px 0 0', fontSize: 13, color: 'var(--mut)' }}>
                A mudança de status é feita pela equipe de suporte.
              </p>
            )}
          </section>

          {/* Histórico */}
          <section className="card card-pad">
            <h2 style={{ margin: '0 0 16px', fontSize: 15 }}>Histórico</h2>

            <ol style={{ listStyle: 'none', margin: 0, padding: 0 }}>
              {chamado.historico.map((evento, i) => (
                <li key={evento.id} style={{ display: 'flex', gap: 12 }}>
                  <div
                    style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}
                    aria-hidden="true"
                  >
                    <span
                      style={{
                        width: 9,
                        height: 9,
                        borderRadius: '50%',
                        background: evento.cor,
                        flexShrink: 0,
                        marginTop: 5,
                      }}
                    />
                    {i < chamado.historico.length - 1 && (
                      <span style={{ width: 1, flex: 1, background: 'var(--bd)', minHeight: 20 }} />
                    )}
                  </div>

                  <div style={{ paddingBottom: 16, minWidth: 0 }}>
                    <span
                      className="mono"
                      style={{ fontSize: 12, color: 'var(--mut)', marginRight: 8 }}
                    >
                      {horaDe(evento.criadoEm)}
                    </span>
                    <span style={{ fontSize: 14, color: 'var(--ink2)' }}>{evento.descricao}</span>
                    {evento.etiqueta && (
                      <span
                        className="badge"
                        style={{
                          marginLeft: 8,
                          background: 'var(--acl)',
                          color: 'var(--acd)',
                          fontSize: 10,
                        }}
                      >
                        {evento.etiqueta}
                      </span>
                    )}
                  </div>
                </li>
              ))}
            </ol>
          </section>

          {/* Comentários */}
          <section className="card card-pad">
            <div
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                marginBottom: 16,
              }}
            >
              <h2 style={{ margin: 0, fontSize: 15 }}>Comentários</h2>
              <span style={{ fontSize: 12, color: 'var(--mut)' }}>
                {chamado.comentarios.length} registro
                {chamado.comentarios.length === 1 ? '' : 's'}
              </span>
            </div>

            {chamado.comentarios.length === 0 ? (
              <p style={{ margin: '0 0 16px', fontSize: 13, color: 'var(--mut)' }}>
                Ainda não há comentários. Use o campo abaixo para registrar uma atualização.
              </p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 20 }}>
                {chamado.comentarios.map((c) => (
                  <div key={c.id} style={{ display: 'flex', gap: 10 }}>
                    <Avatar iniciais={c.iniciais} destaque={c.papel === 'ADMIN'} />
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div
                        style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}
                      >
                        <span style={{ fontSize: 13, fontWeight: 700 }}>{c.autor}</span>
                        <span
                          className="badge"
                          style={{
                            background: c.papel === 'ADMIN' ? 'var(--acl)' : 'var(--ntl)',
                            color: c.papel === 'ADMIN' ? 'var(--acd)' : 'var(--mut)',
                            fontSize: 10,
                          }}
                        >
                          {c.papel}
                        </span>
                        <span style={{ fontSize: 12, color: 'var(--mut)' }}>
                          {dataCompleta(c.criadoEm)}
                        </span>
                      </div>
                      <p
                        style={{
                          margin: '5px 0 0',
                          fontSize: 14,
                          lineHeight: 1.6,
                          color: 'var(--ink2)',
                          whiteSpace: 'pre-wrap',
                        }}
                      >
                        {c.texto}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            <div className="field">
              <label className="label" htmlFor="comentario">
                Adicionar comentário
              </label>
              <textarea
                id="comentario"
                className="textarea"
                style={{ minHeight: 90 }}
                value={rascunho}
                onChange={(e) => setRascunho(e.target.value)}
                placeholder="Escreva uma atualização…"
                maxLength={2000}
              />
            </div>

            <button
              type="button"
              className="btn btn-primary btn-sm"
              style={{ marginTop: 10 }}
              disabled={!rascunho.trim() || processando}
              onClick={() =>
                executar(async () => {
                  await apiComentarios.adicionar(chamado.id, rascunho.trim());
                  setRascunho('');
                }, 'Comentário adicionado', `#${chamado.id} · atualização registrada`)
              }
            >
              Adicionar comentário
            </button>
          </section>
        </div>

        {/* -------------------------------------------------------------- */}
        {/* Coluna lateral                                                  */}
        {/* -------------------------------------------------------------- */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          {/* Triagem inteligente */}
          <section className="card card-pad">
            <h2 style={{ margin: '0 0 4px', fontSize: 15 }}>✨ Triagem inteligente</h2>
            <p style={{ margin: '0 0 16px', fontSize: 12, color: 'var(--mut)' }}>
              Classificação automática {t.provedor ? `via ${t.provedor}` : ''}
            </p>

            {/* Sugestão x final: o ponto do requisito é deixar a diferença explícita */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                gap: 10,
                marginBottom: 14,
              }}
            >
              <div
                style={{
                  padding: 10,
                  borderRadius: 8,
                  background: 'var(--acl)',
                  border: '1px solid #c7d7fb',
                }}
              >
                <p style={{ margin: 0, fontSize: 11, fontWeight: 700, color: 'var(--acd)' }}>
                  SUGESTÃO DA IA
                </p>
                <p style={{ margin: '6px 0 0', fontSize: 13, fontWeight: 700 }}>
                  {t.categoriaSugerida ?? '—'}
                </p>
                <p style={{ margin: '2px 0 0', fontSize: 13 }}>
                  {t.prioridadeSugerida ? ROTULO_PRIORIDADE[t.prioridadeSugerida] : '—'}
                </p>
              </div>

              <div
                style={{
                  padding: 10,
                  borderRadius: 8,
                  background: t.divergente ? 'var(--mdl)' : 'var(--ntl)',
                  border: `1px solid ${t.divergente ? '#f0d3a8' : 'var(--bd)'}`,
                }}
              >
                <p
                  style={{
                    margin: 0,
                    fontSize: 11,
                    fontWeight: 700,
                    color: t.divergente ? '#8a4008' : 'var(--ink2)',
                  }}
                >
                  CLASSIFICAÇÃO FINAL
                </p>
                <p style={{ margin: '6px 0 0', fontSize: 13, fontWeight: 700 }}>
                  {t.categoriaFinal}
                </p>
                <p style={{ margin: '2px 0 0', fontSize: 13 }}>
                  {ROTULO_PRIORIDADE[t.prioridadeFinal]}
                </p>
              </div>
            </div>

            {t.confianca && (
              <div style={{ marginBottom: 12 }}>
                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    fontSize: 12,
                    marginBottom: 4,
                  }}
                >
                  <span style={{ color: 'var(--mut)' }}>Confiança</span>
                  <span style={{ fontWeight: 700 }}>{ROTULO_CONFIANCA[t.confianca]}</span>
                </div>
                <div style={{ height: 6, background: 'var(--ntl)', borderRadius: 999 }}>
                  <div
                    style={{
                      height: '100%',
                      width: `${t.confiancaPercentual ?? 0}%`,
                      background: 'var(--ac)',
                      borderRadius: 999,
                    }}
                  />
                </div>
              </div>
            )}

            <p
              style={{
                margin: '0 0 14px',
                padding: '10px 12px',
                borderRadius: 8,
                background: t.revisada ? 'var(--lol)' : 'var(--ntl)',
                border: `1px solid ${t.revisada ? '#b9e0d6' : 'var(--bd)'}`,
                fontSize: 12,
                color: t.revisada ? '#0b5f58' : 'var(--ink2)',
              }}
            >
              {t.origem === 'MANUAL'
                ? 'Classificação corrigida manualmente pelo suporte'
                : t.revisada
                  ? 'Sugestão da IA aceita pelo suporte'
                  : 'Aguardando revisão do suporte'}
            </p>

            {t.justificativa && (
              <p
                style={{
                  margin: '0 0 14px',
                  fontSize: 12,
                  lineHeight: 1.6,
                  color: 'var(--mut)',
                }}
              >
                {t.justificativa}
              </p>
            )}

            {/* Ações exclusivas do ADMIN */}
            {admin ? (
              corrigindo ? (
                <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                  <div className="field">
                    <label className="label" htmlFor="corr-cat">
                      Categoria
                    </label>
                    <select
                      id="corr-cat"
                      className="select"
                      value={corrCategoria}
                      onChange={(e) => setCorrCategoria(e.target.value as Categoria)}
                    >
                      {CATEGORIAS.map((c) => (
                        <option key={c} value={c}>
                          {c.charAt(0) + c.slice(1).toLowerCase()}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="field">
                    <label className="label" htmlFor="corr-prio">
                      Prioridade
                    </label>
                    <select
                      id="corr-prio"
                      className="select"
                      value={corrPrioridade}
                      onChange={(e) => setCorrPrioridade(e.target.value as Prioridade)}
                    >
                      {PRIORIDADES.map((p) => (
                        <option key={p} value={p}>
                          {ROTULO_PRIORIDADE[p]}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div style={{ display: 'flex', gap: 8 }}>
                    <button
                      type="button"
                      className="btn btn-primary btn-sm"
                      disabled={processando}
                      onClick={() =>
                        executar(async () => {
                          const r = await chamados.corrigirTriagem(
                            chamado.id,
                            corrCategoria,
                            corrPrioridade,
                          );
                          setCorrigindo(false);
                          return r;
                        }, 'Classificação corrigida', `#${chamado.id}`)
                      }
                    >
                      Salvar correção
                    </button>
                    <button
                      type="button"
                      className="btn btn-secondary btn-sm"
                      onClick={() => setCorrigindo(false)}
                    >
                      Cancelar
                    </button>
                  </div>
                </div>
              ) : (
                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={processando || (t.revisada && t.origem === 'IA')}
                    onClick={() =>
                      executar(
                        () => chamados.aceitarTriagem(chamado.id),
                        'Classificação da IA aceita',
                        `#${chamado.id}`,
                      )
                    }
                  >
                    {t.revisada && t.origem === 'IA' ? 'Classificação aceita' : 'Aceitar sugestão'}
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    onClick={() => {
                      setCorrCategoria(t.categoriaFinal);
                      setCorrPrioridade(t.prioridadeFinal);
                      setCorrigindo(true);
                    }}
                  >
                    Corrigir
                  </button>
                </div>
              )
            ) : (
              <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>
                A revisão da classificação é feita pela equipe de suporte.
              </p>
            )}
          </section>

          {/* Responsável */}
          <section className="card card-pad">
            <h2 style={{ margin: '0 0 12px', fontSize: 15 }}>Responsável</h2>

            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <Avatar iniciais={chamado.responsavel?.iniciais ?? ''} destaque={!!chamado.responsavel} />
              <div>
                <p style={{ margin: 0, fontSize: 14, fontWeight: 700 }}>
                  {chamado.responsavel?.nome ?? 'Não atribuído'}
                </p>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>
                  {chamado.responsavel ? 'Equipe de suporte · ADMIN' : 'Nenhum responsável definido'}
                </p>
              </div>
            </div>

            {admin && !chamado.encerrado && (
              <>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  style={{ marginTop: 12 }}
                  onClick={() => setAtribuindo((v) => !v)}
                >
                  {chamado.responsavel ? 'Alterar responsável' : 'Atribuir responsável'}
                </button>

                {atribuindo && (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginTop: 10 }}>
                    {responsaveis.map((r) => (
                      <button
                        key={r.id}
                        type="button"
                        disabled={processando}
                        onClick={() =>
                          executar(async () => {
                            const atualizado = await chamados.atribuirResponsavel(chamado.id, r.id);
                            setAtribuindo(false);
                            return atualizado;
                          }, 'Chamado atribuído', `#${chamado.id} · ${r.nome}`)
                        }
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: 8,
                          padding: '8px 10px',
                          border: `1px solid ${
                            chamado.responsavel?.id === r.id ? '#c7d7fb' : 'var(--bd)'
                          }`,
                          background:
                            chamado.responsavel?.id === r.id ? 'var(--acl)' : 'var(--sf)',
                          borderRadius: 8,
                          cursor: 'pointer',
                          textAlign: 'left',
                        }}
                      >
                        <Avatar iniciais={r.iniciais} tamanho={26} />
                        <span style={{ fontSize: 13, fontWeight: 600 }}>{r.nome}</span>
                      </button>
                    ))}
                  </div>
                )}
              </>
            )}

            {!admin && (
              <p style={{ margin: '10px 0 0', fontSize: 12, color: 'var(--mut)' }}>
                A atribuição é feita pela equipe de suporte.
              </p>
            )}
          </section>
        </div>
      </div>

      <Modal
        aberto={confirmandoCancelamento}
        titulo="Cancelar chamado?"
        descricao="Esta ação cancela o chamado e não poderá ser desfeita. O histórico permanece disponível para consulta."
        rotuloConfirmar="Confirmar cancelamento"
        tomConfirmar="danger"
        processando={processando}
        aoFechar={() => setConfirmandoCancelamento(false)}
        aoConfirmar={() =>
          executar(async () => {
            await chamados.cancelar(chamado.id);
            setConfirmandoCancelamento(false);
          }, 'Chamado cancelado', `#${chamado.id} · ação registrada no histórico`)
        }
      />

      <style>{`
        @media (max-width: 1000px) {
          .detalhe-grid { grid-template-columns: 1fr !important; }
        }
      `}</style>
    </div>
  );
}
