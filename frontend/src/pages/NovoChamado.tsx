import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { ErroApi } from '../api/erros';
import { chamados } from '../api/servicos';
import type { ChamadoDetalhe } from '../api/tipos';
import { CORES_STATUS, PriorityBadge } from '../components/Badges';
import { Icone } from '../components/Icone';
import { ROTULO_CONFIANCA } from '../utils/formato';

type Etapa = 'formulario' | 'analisando' | 'concluido';

const EXEMPLO = {
  titulo: 'Impressora do 3º andar não está funcionando',
  descricao:
    'A impressora do 3º andar não imprime desde ontem. Aparece luz vermelha no painel e o setor ' +
    'inteiro está sem imprimir os empenhos do dia.',
};

const PASSOS = [
  'Texto recebido e normalizado',
  'Categoria identificada',
  'Prioridade calculada',
];

export function NovoChamado() {
  const navegar = useNavigate();

  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [etapa, setEtapa] = useState<Etapa>('formulario');
  const [passo, setPasso] = useState(0);
  const [resultado, setResultado] = useState<ChamadoDetalhe | null>(null);
  const [erro, setErro] = useState<string | null>(null);
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});

  const temporizadores = useRef<number[]>([]);

  useEffect(() => {
    const atuais = temporizadores.current;
    return () => atuais.forEach(clearTimeout);
  }, []);

  const valido = titulo.trim().length > 0 && descricao.trim().length > 10;

  async function aoEnviar(e: FormEvent) {
    e.preventDefault();
    if (!valido) return;

    setErro(null);
    setErrosCampo({});
    setEtapa('analisando');
    setPasso(0);

    // A animação da triagem roda em paralelo à chamada real: ela ilustra as
    // etapas da classificação sem atrasar artificialmente a resposta.
    temporizadores.current.push(window.setTimeout(() => setPasso(1), 700));
    temporizadores.current.push(window.setTimeout(() => setPasso(2), 1400));

    const inicio = Date.now();

    try {
      const criado = await chamados.criar(titulo.trim(), descricao.trim());

      // Garante que a etapa de análise seja perceptível mesmo quando a API
      // responde instantaneamente.
      const restante = Math.max(0, 1800 - (Date.now() - inicio));
      temporizadores.current.push(
        window.setTimeout(() => {
          setResultado(criado);
          setEtapa('concluido');
        }, restante),
      );
    } catch (e) {
      setEtapa('formulario');
      if (e instanceof ErroApi) {
        setErro(e.mensagem);
        setErrosCampo(e.porCampo);
      } else {
        setErro('Não foi possível abrir o chamado. Tente novamente.');
      }
    }
  }

  function recomecar() {
    setTitulo('');
    setDescricao('');
    setResultado(null);
    setEtapa('formulario');
  }

  // ---------------------------------------------------------------- análise
  if (etapa === 'analisando') {
    return (
      <div style={{ maxWidth: 640 }}>
        <div className="card card-pad" style={{ textAlign: 'center' }}>
          <div
            aria-hidden="true"
            style={{
              width: 52,
              height: 52,
              margin: '0 auto 16px',
              borderRadius: '50%',
              background: 'var(--acl)',
              display: 'grid',
              placeItems: 'center',
              animation: 'pls 1.8s infinite',
            }}
          >
            <Icone nome="brilho" tamanho={26} traco={1.7} cor="var(--ac)" />
          </div>

          <h1 style={{ margin: 0, fontSize: 19 }}>Analisando sua solicitação…</h1>
          <p style={{ margin: '6px 0 24px', fontSize: 14, color: 'var(--mut)' }}>
            A triagem automática está lendo o texto para sugerir categoria e prioridade.
          </p>

          <div style={{ display: 'flex', flexDirection: 'column', gap: 10, textAlign: 'left' }}>
            {PASSOS.map((rotulo, i) => {
              const concluido = passo > i;
              const atual = passo === i;
              return (
                <div
                  key={rotulo}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 12px',
                    borderRadius: 8,
                    border: `1px solid ${concluido ? 'var(--lob)' : atual ? 'var(--acb)' : 'var(--bd)'}`,
                    background: concluido ? 'var(--lol)' : atual ? 'var(--acl)' : 'var(--sf2)',
                    color: concluido ? 'var(--lof)' : atual ? 'var(--acd)' : 'var(--mut)',
                    fontSize: 14,
                  }}
                >
                  {concluido ? (
                    <Icone nome="check" tamanho={11} traco={3} />
                  ) : (
                    <span aria-hidden="true" style={{ fontWeight: 700 }}>
                      ·
                    </span>
                  )}
                  {rotulo}
                </div>
              );
            })}
          </div>
          <span className="sr-only" role="status">
            Analisando a solicitação
          </span>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------- concluído
  if (etapa === 'concluido' && resultado) {
    const t = resultado.triagem;

    return (
      <div style={{ maxWidth: 640 }}>
        <div className="card card-pad">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 6 }}>
            <span
              aria-hidden="true"
              style={{
                width: 30,
                height: 30,
                borderRadius: '50%',
                background: 'var(--lol)',
                color: 'var(--lof)',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <Icone nome="check" tamanho={15} traco={3} cor="var(--lof)" />
            </span>
            <h1 style={{ margin: 0, fontSize: 19 }}>Triagem concluída</h1>
          </div>

          <p style={{ margin: '0 0 20px', fontSize: 14, color: 'var(--mut)' }}>
            Chamado <strong className="mono">#{resultado.id}</strong> aberto. A equipe de suporte
            pode revisar a classificação sugerida.
          </p>

          <div
            style={{
              border: '1px solid var(--acb)',
              background: 'var(--acl)',
              borderRadius: 8,
              padding: 16,
            }}
          >
            <p
              style={{
                margin: '0 0 14px',
                fontSize: 12,
                fontWeight: 800,
                color: 'var(--acd)',
                letterSpacing: '0.03em',
                display: 'flex',
                alignItems: 'center',
                gap: 6,
              }}
            >
              <Icone nome="brilho" tamanho={14} traco={1.8} />
              SUGESTÃO DA IA
            </p>

            <div style={{ display: 'flex', gap: 28, flexWrap: 'wrap' }}>
              <div>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>Categoria sugerida</p>
                <p style={{ margin: '3px 0 0', fontSize: 15, fontWeight: 700 }}>
                  {resultado.categoriaRotulo}
                </p>
              </div>
              <div>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>Prioridade sugerida</p>
                <div style={{ marginTop: 4 }}>
                  <PriorityBadge prioridade={resultado.prioridade} />
                </div>
              </div>
              <div>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>Confiança</p>
                <p style={{ margin: '3px 0 0', fontSize: 15, fontWeight: 700 }}>
                  {t.confianca ? ROTULO_CONFIANCA[t.confianca] : '—'}
                </p>
              </div>
            </div>

            {t.confiancaPercentual != null && (
              <div style={{ height: 6, background: 'var(--sf)', borderRadius: 999, marginTop: 14 }}>
                <div
                  style={{
                    height: '100%',
                    width: `${t.confiancaPercentual}%`,
                    background: 'var(--ac)',
                    borderRadius: 999,
                    animation: 'bar .6s ease',
                  }}
                />
              </div>
            )}

            {t.justificativa && (
              <p
                style={{
                  margin: '14px 0 0',
                  fontSize: 13,
                  color: 'var(--ink2)',
                  lineHeight: 1.6,
                  paddingTop: 12,
                  borderTop: '1px solid var(--acb)',
                }}
              >
                <strong>Por que esta classificação:</strong> {t.justificativa}
              </p>
            )}
          </div>

          {resultado.possiveisDuplicados.length > 0 && (
            <div
              style={{
                border: '1px solid var(--md)',
                background: 'var(--mdl)',
                borderRadius: 8,
                padding: 16,
                marginTop: 16,
              }}
            >
              <p
                style={{
                  margin: '0 0 6px',
                  fontSize: 12,
                  fontWeight: 800,
                  color: 'var(--md)',
                  letterSpacing: '0.03em',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 6,
                }}
              >
                <Icone nome="duplicado" tamanho={14} traco={1.8} />
                POSSÍVEL DUPLICADO
              </p>
              <p style={{ margin: '0 0 12px', fontSize: 13, color: 'var(--ink2)', lineHeight: 1.6 }}>
                {resultado.possiveisDuplicados.length === 1
                  ? 'Um chamado já registrado descreve praticamente o mesmo problema. '
                  : 'Alguns chamados já registrados descrevem praticamente o mesmo problema. '}
                Se for o mesmo incidente, acompanhe o existente em vez de aguardar um novo
                atendimento.
              </p>

              <ul style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: 6 }}>
                {resultado.possiveisDuplicados.map((similar) => (
                  <li key={similar.id}>
                    <button
                      type="button"
                      onClick={() => navegar(`/chamados/${similar.id}`)}
                      style={{
                        width: '100%',
                        textAlign: 'left',
                        display: 'flex',
                        alignItems: 'center',
                        gap: 10,
                        padding: '9px 11px',
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
                      >
                        {similar.similaridade}%
                      </span>
                      <span
                        style={{
                          minWidth: 0,
                          flex: 1,
                          fontSize: 13,
                          fontWeight: 600,
                          overflow: 'hidden',
                          textOverflow: 'ellipsis',
                          whiteSpace: 'nowrap',
                        }}
                      >
                        #{similar.id} — {similar.titulo}
                      </span>
                      <span style={{ fontSize: 12, color: CORES_STATUS[similar.status].cor, flexShrink: 0 }}>
                        {similar.statusRotulo}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div style={{ display: 'flex', gap: 8, marginTop: 20, flexWrap: 'wrap' }}>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navegar(`/chamados/${resultado.id}`)}
            >
              Ver chamado
            </button>
            <button type="button" className="btn btn-secondary" onClick={recomecar}>
              Abrir outro chamado
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ------------------------------------------------------------- formulário
  return (
    <div style={{ maxWidth: 840 }}>
      <button
        type="button"
        onClick={() => navegar('/chamados')}
        style={{
          border: 0,
          background: 'transparent',
          padding: 0,
          marginBottom: 10,
          fontSize: 12.5,
          color: 'var(--mut)',
          cursor: 'pointer',
        }}
      >
        ‹ Voltar
      </button>

      <h1 style={{ margin: 0, fontSize: 26, fontWeight: 800, letterSpacing: '-0.025em' }}>
        Novo chamado
      </h1>
      <p style={{ margin: '5px 0 18px', fontSize: 14, color: 'var(--mut)' }}>
        Descreva o problema com suas palavras — deixe nossa IA classificar sua solicitação
        automaticamente.
      </p>

      <div className="card" style={{ overflow: 'hidden' }}>
        {/* A nota da IA abre o cartao, como no design: ela responde a duvida
            que aparece antes de a pessoa comecar a escrever ("onde escolho a
            categoria?"), e nao depois de o formulario inteiro ter passado. */}
        <div
          style={{
            display: 'flex',
            gap: 11,
            alignItems: 'flex-start',
            padding: '14px 18px',
            background: 'var(--acl)',
            borderBottom: '1px solid var(--acb)',
          }}
        >
          <Icone nome="brilho" tamanho={17} traco={1.8} cor="var(--ac)" style={{ marginTop: 1 }} />
          <div>
            <p style={{ margin: 0, fontSize: 13, fontWeight: 700, color: 'var(--acd)' }}>
              Você não precisa escolher categoria nem prioridade
            </p>
            <p style={{ margin: 0, fontSize: 12.5, color: 'var(--acf)', lineHeight: 1.45 }}>
              A triagem inteligente analisa o texto e sugere a classificação. A equipe de suporte
              revisa depois.
            </p>
          </div>
        </div>

        <div style={{ padding: '20px 18px' }}>
        {erro && (
          <div
            role="alert"
            style={{
              background: 'var(--hil)',
              border: '1px solid var(--hib)',
              color: 'var(--hif)',
              borderRadius: 8,
              padding: '10px 12px',
              fontSize: 13,
              marginBottom: 16,
            }}
          >
            {erro}
          </div>
        )}

        <form onSubmit={aoEnviar} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <div className="field">
            <label className="label" htmlFor="titulo">
              Título <span style={{ color: 'var(--hi)' }}>*</span>
            </label>
            <input
              id="titulo"
              className={`input ${errosCampo.titulo ? 'input-erro' : ''}`}
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              placeholder="Ex.: Impressora do 3º andar não está funcionando"
              maxLength={200}
              required
            />
            <span className="hint">Resuma o problema em uma frase.</span>
            {errosCampo.titulo && <span className="erro-campo">{errosCampo.titulo}</span>}
          </div>

          <div className="field">
            <label className="label" htmlFor="descricao">
              Descrição <span style={{ color: 'var(--hi)' }}>*</span>
            </label>
            <textarea
              id="descricao"
              className={`textarea ${errosCampo.descricao ? 'input-erro' : ''}`}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              placeholder="Conte o que aconteceu, desde quando, e o impacto no seu trabalho."
              maxLength={4000}
              required
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
              <span className="hint">Quanto mais contexto, melhor a triagem.</span>
              <span className="hint mono">{descricao.length}/4000</span>
            </div>
            {errosCampo.descricao && <span className="erro-campo">{errosCampo.descricao}</span>}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
            {['Categoria', 'Prioridade'].map((campo) => (
              <div
                key={campo}
                style={{
                  border: '1px dashed var(--bd)',
                  borderRadius: 'var(--rd)',
                  padding: '12px 13px',
                  background: 'var(--sf2)',
                }}
              >
                <p
                  style={{
                    margin: '0 0 5px',
                    fontSize: 11,
                    fontWeight: 700,
                    letterSpacing: '0.05em',
                    textTransform: 'uppercase',
                    color: 'var(--mut)',
                  }}
                >
                  {campo}
                </p>
                <p
                  style={{
                    margin: 0,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 7,
                    fontSize: 13,
                    color: 'var(--mut)',
                  }}
                >
                  <span
                    aria-hidden="true"
                    style={{
                      width: 7,
                      height: 7,
                      borderRadius: '50%',
                      background: 'var(--dim)',
                      flex: 'none',
                    }}
                  />
                  Definida pela IA
                </p>
              </div>
            ))}
          </div>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="submit" className="btn btn-primary" disabled={!valido}>
              Enviar chamado
            </button>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => {
                setTitulo(EXEMPLO.titulo);
                setDescricao(EXEMPLO.descricao);
              }}
            >
              Preencher com um exemplo
            </button>
          </div>
          </form>
        </div>
      </div>
    </div>
  );
}
