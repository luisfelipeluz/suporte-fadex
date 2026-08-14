/**
 * Quadro Kanban do fluxo de chamados, apresentado em modal.
 *
 * As colunas espelham exatamente o ciclo de vida do domínio
 * (`StatusChamado`): ABERTO ↔ EM ANDAMENTO ↔ RESOLVIDO → FECHADO. Arrastar um
 * cartão para uma coluna vizinha dispara a mesma operação de mudança de status
 * usada na tela de detalhe — o quadro é uma forma de operar o fluxo, não uma
 * visualização paralela com regras próprias.
 *
 * O backend aceita uma etapa por vez em qualquer direção, então a interface só
 * oferece como alvo válido as colunas imediatamente vizinhas: o usuário não
 * descobre a regra por tentativa e erro. FECHADO não devolve cartão para
 * RESOLVIDO porque isso seria a reabertura que a regra de negócio proíbe.
 */

import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { ErroApi } from '../api/erros';
import { chamados as apiChamados } from '../api/servicos';
import type { ChamadoResumo, StatusChamado } from '../api/tipos';
import { useAuth } from '../auth/AuthContext';
import { useRealtime } from '../realtime/RealtimeContext';
import { dataRelativa, ICONE_STATUS, ROTULO_STATUS } from '../utils/formato';
import { Avatar, CORES_STATUS, PriorityBadge } from './Badges';
import { EstadoErro } from './Estados';
import { useToasts } from './Toasts';

/** Colunas do quadro. CANCELADO fica fora: não é etapa do fluxo, é saída dele. */
const COLUNAS: StatusChamado[] = ['ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO'];

/**
 * O quadro carrega os chamados de uma vez só para poder distribuí-los entre as
 * colunas; a paginação da listagem não faz sentido aqui.
 */
const LIMITE_CARTOES = 200;

/** Próxima etapa do fluxo, ou `null` se a coluna já for a última. */
function proximoDe(status: StatusChamado): StatusChamado | null {
  const i = COLUNAS.indexOf(status);
  return i >= 0 && i < COLUNAS.length - 1 ? COLUNAS[i + 1] : null;
}

/**
 * Etapa anterior do fluxo, ou `null` se não houver volta.
 *
 * Espelha `StatusChamado.anterior()` no backend, inclusive na exceção que
 * importa: de FECHADO não se retorna, porque reabrir chamado encerrado é
 * proibido pela regra de negócio.
 */
function anteriorDe(status: StatusChamado): StatusChamado | null {
  if (status === 'FECHADO') return null;
  const i = COLUNAS.indexOf(status);
  return i > 0 ? COLUNAS[i - 1] : null;
}

/**
 * Explica para onde o cartão poderia ir, em vez de apenas recusar o movimento.
 *
 * Só aparece em caminhos que o arraste já bloqueia (toque, solte fora de hora):
 * a interface evita o erro, mas quando ele acontece o motivo fica dito.
 */
function destinoInvalidoExplicado(
  origem: StatusChamado,
  avanco: StatusChamado | null,
  retorno: StatusChamado | null,
): string {
  const destinos = [avanco, retorno].filter((s): s is StatusChamado => s != null);

  if (destinos.length === 0) {
    return `Chamados em ${ROTULO_STATUS[origem]} não podem ser movidos.`;
  }

  return (
    `De ${ROTULO_STATUS[origem]} o chamado só pode ir para ` +
    `${destinos.map((s) => ROTULO_STATUS[s]).join(' ou ')}.`
  );
}

export function QuadroKanban({ aberto, aoFechar }: { aberto: boolean; aoFechar: () => void }) {
  const { admin } = useAuth();
  const { revisao } = useRealtime();
  const { notificar } = useToasts();
  const navegar = useNavigate();

  const [cartoes, setCartoes] = useState<ChamadoResumo[]>([]);
  const [total, setTotal] = useState(0);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<unknown>(null);

  /** Cartão em arraste; guardado em estado porque o `dataTransfer` não é
   * legível durante o `dragover` — é ele que define qual coluna aceita o solte. */
  const [arrastando, setArrastando] = useState<ChamadoResumo | null>(null);
  const [colunaAlvo, setColunaAlvo] = useState<StatusChamado | null>(null);
  const [movendo, setMovendo] = useState<number | null>(null);

  const carregar = useCallback(() => {
    setCarregando(true);
    setErro(null);

    apiChamados
      .listar({ size: LIMITE_CARTOES })
      .then((pagina) => {
        setCartoes(pagina.conteudo);
        setTotal(pagina.totalElementos);
      })
      .catch(setErro)
      .finally(() => setCarregando(false));
  }, []);

  useEffect(() => {
    if (aberto) carregar();
  }, [aberto, carregar]);

  // Mantém o quadro alinhado com o que outras sessões estão fazendo.
  useEffect(() => {
    if (aberto && revisao > 0) carregar();
  }, [aberto, revisao, carregar]);

  useEffect(() => {
    if (!aberto) return;

    const aoTeclar = (e: KeyboardEvent) => {
      if (e.key === 'Escape') aoFechar();
    };
    document.addEventListener('keydown', aoTeclar);

    // Evita a página de trás rolar junto com o quadro.
    const overflowAnterior = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    return () => {
      document.removeEventListener('keydown', aoTeclar);
      document.body.style.overflow = overflowAnterior;
    };
  }, [aberto, aoFechar]);

  const porColuna = useMemo(() => {
    const mapa = Object.fromEntries(COLUNAS.map((s) => [s, [] as ChamadoResumo[]])) as Record<
      StatusChamado,
      ChamadoResumo[]
    >;
    cartoes.forEach((c) => mapa[c.status]?.push(c));
    return mapa;
  }, [cartoes]);

  const cancelados = cartoes.filter((c) => c.status === 'CANCELADO').length;
  const noQuadro = cartoes.length - cancelados;

  /**
   * Move o chamado uma etapa, para a frente ou de volta.
   *
   * A troca é aplicada na hora e desfeita se a API recusar: o arraste responde
   * imediatamente sem que o quadro possa ficar mostrando um estado que o
   * servidor não aceitou.
   */
  async function mover(chamado: ChamadoResumo, destino: StatusChamado) {
    const avanco = proximoDe(chamado.status);
    const retorno = anteriorDe(chamado.status);

    if (destino !== avanco && destino !== retorno) {
      notificar(
        'Movimentação não permitida',
        destinoInvalidoExplicado(chamado.status, avanco, retorno),
        'alerta',
      );
      return;
    }

    const ehRetorno = destino === retorno;

    const anteriores = cartoes;
    setMovendo(chamado.id);
    setCartoes((atuais) =>
      atuais.map((c) => (c.id === chamado.id ? { ...c, status: destino } : c)),
    );

    try {
      await apiChamados.alterarStatus(chamado.id, destino);
      notificar(
        ehRetorno ? 'Chamado retornado' : 'Chamado movido',
        `#${chamado.id} · ${ROTULO_STATUS[destino]}`,
        'sucesso',
      );
      // Recarrega para trazer o que a operação mudou além do status — o
      // responsável assumido automaticamente ao entrar em andamento, por exemplo.
      carregar();
    } catch (e) {
      setCartoes(anteriores);
      notificar(
        'Não foi possível mover',
        e instanceof ErroApi ? e.mensagem : 'Tente novamente em instantes.',
        'erro',
      );
    } finally {
      setMovendo(null);
    }
  }

  function abrirChamado(id: number) {
    aoFechar();
    navegar(`/chamados/${id}`);
  }

  if (!aberto) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Quadro Kanban dos chamados"
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
          width: 'min(1180px, 100%)',
          maxHeight: 'min(860px, calc(100vh - 40px))',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: 'var(--sh3)',
          animation: 'inU .2s ease',
          overflow: 'hidden',
        }}
      >
        {/* Cabeçalho */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--bd)',
            display: 'flex',
            alignItems: 'flex-start',
            justifyContent: 'space-between',
            gap: 12,
          }}
        >
          <div>
            <h2 style={{ margin: 0, fontSize: 17 }}>Quadro Kanban</h2>
            <p style={{ margin: '3px 0 0', fontSize: 13, color: 'var(--mut)' }}>
              {admin
                ? 'Arraste um chamado para a coluna vizinha: avança o status ou retorna para a etapa anterior.'
                : 'Acompanhe em que etapa do fluxo está cada chamado.'}
            </p>
          </div>

          <button type="button" className="btn btn-secondary btn-sm" onClick={aoFechar}>
            Fechar
          </button>
        </div>

        {/* Colunas */}
        {erro ? (
          <EstadoErro erro={erro} aoTentarNovamente={carregar} />
        ) : (
          <div
            style={{
              display: 'flex',
              gap: 12,
              padding: 16,
              overflowX: 'auto',
              flex: 1,
              minHeight: 0,
              alignItems: 'stretch',
            }}
          >
            {COLUNAS.map((coluna) => {
              const cor = CORES_STATUS[coluna];
              const lista = porColuna[coluna];

              // Só as etapas vizinhas à do cartão arrastado aceitam o solte.
              const recebeRetorno = arrastando ? anteriorDe(arrastando.status) === coluna : false;
              const aceita = arrastando
                ? proximoDe(arrastando.status) === coluna || recebeRetorno
                : false;
              const recusa = arrastando != null && !aceita && arrastando.status !== coluna;
              const destacada = aceita && colunaAlvo === coluna;

              return (
                <section
                  key={coluna}
                  onDragOver={(e) => {
                    if (!aceita) return;
                    // Sem o preventDefault o navegador não considera a área um alvo válido.
                    e.preventDefault();
                    e.dataTransfer.dropEffect = 'move';
                    setColunaAlvo(coluna);
                  }}
                  onDragLeave={(e) => {
                    // Ignora a saída para um filho da própria coluna.
                    if (e.currentTarget.contains(e.relatedTarget as Node | null)) return;
                    setColunaAlvo((atual) => (atual === coluna ? null : atual));
                  }}
                  onDrop={(e) => {
                    e.preventDefault();
                    const cartao = arrastando;
                    setArrastando(null);
                    setColunaAlvo(null);
                    if (cartao) mover(cartao, coluna);
                  }}
                  style={{
                    width: 276,
                    flexShrink: 0,
                    display: 'flex',
                    flexDirection: 'column',
                    background: destacada ? cor.bg : 'var(--bg)',
                    border: `1px solid ${destacada ? cor.cor : 'var(--bd)'}`,
                    borderRadius: 'var(--rd)',
                    opacity: recusa ? 0.45 : 1,
                    transition: 'background .15s ease, border-color .15s ease, opacity .15s ease',
                  }}
                >
                  <header
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      padding: '10px 12px',
                      borderBottom: '1px solid var(--bd)',
                    }}
                  >
                    <span aria-hidden="true" style={{ color: cor.cor }}>
                      {ICONE_STATUS[coluna]}
                    </span>
                    <span style={{ fontSize: 12, fontWeight: 700, color: cor.fg }}>
                      {ROTULO_STATUS[coluna]}
                    </span>
                    <span
                      className="badge"
                      style={{ marginLeft: 'auto', background: cor.bg, color: cor.fg }}
                    >
                      {lista.length}
                    </span>
                  </header>

                  <div
                    style={{
                      display: 'flex',
                      flexDirection: 'column',
                      gap: 8,
                      padding: 10,
                      overflowY: 'auto',
                      flex: 1,
                      minHeight: 120,
                    }}
                  >
                    {lista.length === 0 ? (
                      <p
                        style={{
                          margin: 'auto 0',
                          padding: '18px 10px',
                          textAlign: 'center',
                          fontSize: 12,
                          color: 'var(--mut)',
                          border: '1px dashed var(--bd)',
                          borderRadius: 'var(--rd)',
                        }}
                      >
                        {destacada
                          ? recebeRetorno
                            ? 'Solte aqui para retornar'
                            : 'Solte aqui para avançar'
                          : 'Nenhum chamado nesta etapa'}
                      </p>
                    ) : (
                      lista.map((c) => {
                        const proximo = proximoDe(c.status);
                        const anterior = anteriorDe(c.status);
                        const arrastavel =
                          admin && (proximo != null || anterior != null) && movendo !== c.id;

                        return (
                          <article
                            key={c.id}
                            draggable={arrastavel}
                            onDragStart={(e) => {
                              setArrastando(c);
                              e.dataTransfer.effectAllowed = 'move';
                              e.dataTransfer.setData('text/plain', String(c.id));
                            }}
                            onDragEnd={() => {
                              setArrastando(null);
                              setColunaAlvo(null);
                            }}
                            onClick={() => abrirChamado(c.id)}
                            style={{
                              background: 'var(--sf)',
                              border: '1px solid var(--bd)',
                              borderLeft: `3px solid ${cor.cor}`,
                              borderRadius: 'var(--rd)',
                              padding: 10,
                              boxShadow: 'var(--sh1)',
                              cursor: arrastavel ? 'grab' : 'pointer',
                              opacity: arrastando?.id === c.id || movendo === c.id ? 0.5 : 1,
                            }}
                          >
                            <div
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 6,
                                marginBottom: 6,
                              }}
                            >
                              <span className="mono" style={{ fontSize: 11, color: 'var(--mut)' }}>
                                #{c.id}
                              </span>
                              <span style={{ marginLeft: 'auto' }}>
                                <PriorityBadge prioridade={c.prioridade} />
                              </span>
                            </div>

                            <p
                              style={{
                                margin: 0,
                                fontSize: 13,
                                fontWeight: 600,
                                color: 'var(--ink)',
                                lineHeight: 1.35,
                              }}
                            >
                              {c.titulo}
                            </p>

                            <p style={{ margin: '6px 0 0', fontSize: 11, color: 'var(--mut)' }}>
                              {c.categoriaRotulo} · {dataRelativa(c.criadoEm)}
                            </p>

                            <div
                              style={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 6,
                                marginTop: 8,
                              }}
                            >
                              <Avatar
                                iniciais={c.responsavel?.iniciais ?? '—'}
                                tamanho={22}
                                destaque={c.responsavel != null}
                              />
                              <span
                                style={{
                                  fontSize: 11,
                                  color: c.responsavel ? 'var(--ink2)' : '#94a3b8',
                                  overflow: 'hidden',
                                  textOverflow: 'ellipsis',
                                  whiteSpace: 'nowrap',
                                }}
                              >
                                {c.responsavel?.nome ?? 'Não atribuído'}
                              </span>

                              {/* Caminho equivalente ao arraste para quem navega
                                  por teclado ou toque. */}
                              {admin && (anterior || proximo) && (
                                <span
                                  style={{ marginLeft: 'auto', display: 'flex', gap: 2 }}
                                  onClick={(e) => e.stopPropagation()}
                                >
                                  {anterior && (
                                    <button
                                      type="button"
                                      className="btn btn-ghost btn-sm"
                                      title={`Retornar para ${ROTULO_STATUS[anterior]}`}
                                      disabled={movendo === c.id}
                                      onClick={() => mover(c, anterior)}
                                      style={{ padding: '2px 8px' }}
                                    >
                                      <span aria-hidden="true">←</span>
                                      <span className="sr-only">
                                        Retornar chamado #{c.id} para {ROTULO_STATUS[anterior]}
                                      </span>
                                    </button>
                                  )}

                                  {proximo && (
                                    <button
                                      type="button"
                                      className="btn btn-ghost btn-sm"
                                      title={`Mover para ${ROTULO_STATUS[proximo]}`}
                                      disabled={movendo === c.id}
                                      onClick={() => mover(c, proximo)}
                                      style={{ padding: '2px 8px' }}
                                    >
                                      <span aria-hidden="true">→</span>
                                      <span className="sr-only">
                                        Mover chamado #{c.id} para {ROTULO_STATUS[proximo]}
                                      </span>
                                    </button>
                                  )}
                                </span>
                              )}
                            </div>
                          </article>
                        );
                      })
                    )}
                  </div>
                </section>
              );
            })}
          </div>
        )}

        {/* Rodapé */}
        <div
          style={{
            padding: '10px 20px',
            borderTop: '1px solid var(--bd)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            gap: 12,
            flexWrap: 'wrap',
            fontSize: 12,
            color: 'var(--mut)',
          }}
        >
          <span>
            {carregando && cartoes.length === 0
              ? 'Carregando chamados…'
              : `${noQuadro} chamado${noQuadro === 1 ? '' : 's'} no quadro`}
            {cancelados > 0 && ` · ${cancelados} cancelado${cancelados === 1 ? '' : 's'} fora do fluxo`}
            {total > cartoes.length && ` · exibindo os ${cartoes.length} mais recentes`}
          </span>

          <span>
            {admin
              ? 'Uma etapa por vez, nos dois sentidos. Chamados fechados não reabrem.'
              : 'A movimentação dos chamados é feita pela equipe de suporte.'}
          </span>
        </div>
      </div>
    </div>
  );
}
