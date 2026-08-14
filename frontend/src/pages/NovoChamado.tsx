import { useEffect, useRef, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';

import { ErroApi } from '../api/erros';
import { chamados } from '../api/servicos';
import type { ChamadoDetalhe } from '../api/tipos';
import { PriorityBadge } from '../components/Badges';
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
                    border: `1px solid ${concluido ? '#b9e0d6' : atual ? '#c7d7fb' : 'var(--bd)'}`,
                    background: concluido ? 'var(--lol)' : atual ? 'var(--acl)' : '#fafbfd',
                    color: concluido ? '#0b5f58' : atual ? 'var(--acd)' : 'var(--mut)',
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
                color: '#0b5f58',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              <Icone nome="check" tamanho={15} traco={3} cor="#0b5f58" />
            </span>
            <h1 style={{ margin: 0, fontSize: 19 }}>Triagem concluída</h1>
          </div>

          <p style={{ margin: '0 0 20px', fontSize: 14, color: 'var(--mut)' }}>
            Chamado <strong className="mono">#{resultado.id}</strong> aberto. A equipe de suporte
            pode revisar a classificação sugerida.
          </p>

          <div
            style={{
              border: '1px solid #c7d7fb',
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
              <div style={{ height: 6, background: '#fff', borderRadius: 999, marginTop: 14 }}>
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
                  borderTop: '1px solid #c7d7fb',
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
                      <span style={{ fontSize: 12, color: similar.statusCor, flexShrink: 0 }}>
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
    <div style={{ maxWidth: 640 }}>
      <h1 style={{ margin: 0, fontSize: 22, letterSpacing: '-0.02em' }}>Novo chamado</h1>
      <p style={{ margin: '4px 0 20px', fontSize: 14, color: 'var(--mut)' }}>
        Descreva o problema com suas palavras. Deixe nossa IA classificar sua solicitação
        automaticamente.
      </p>

      <div className="card card-pad">
        {erro && (
          <div
            role="alert"
            style={{
              background: 'var(--hil)',
              border: '1px solid #f6c9c9',
              color: '#b3261e',
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
              Título
            </label>
            <input
              id="titulo"
              className={`input ${errosCampo.titulo ? 'input-erro' : ''}`}
              value={titulo}
              onChange={(e) => setTitulo(e.target.value)}
              placeholder="Resuma o problema em uma frase"
              maxLength={200}
              required
            />
            {errosCampo.titulo && <span className="erro-campo">{errosCampo.titulo}</span>}
          </div>

          <div className="field">
            <label className="label" htmlFor="descricao">
              Descrição
            </label>
            <textarea
              id="descricao"
              className={`textarea ${errosCampo.descricao ? 'input-erro' : ''}`}
              value={descricao}
              onChange={(e) => setDescricao(e.target.value)}
              placeholder="O que aconteceu, desde quando, e quem está sendo afetado?"
              maxLength={4000}
              required
            />
            <div style={{ display: 'flex', justifyContent: 'space-between', gap: 12 }}>
              <span className="hint">
                Quanto mais contexto, melhor a classificação automática.
              </span>
              <span className="hint mono">{descricao.length}/4000</span>
            </div>
            {errosCampo.descricao && <span className="erro-campo">{errosCampo.descricao}</span>}
          </div>

          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              padding: '12px 14px',
              background: 'var(--acl)',
              border: '1px solid #c7d7fb',
              borderRadius: 8,
            }}
          >
            <Icone nome="brilho" tamanho={17} traco={1.8} cor="var(--ac)" />
            <p style={{ margin: 0, fontSize: 13, color: 'var(--ink2)', lineHeight: 1.5 }}>
              Você não precisa escolher categoria nem prioridade: a IA analisa o texto e sugere a
              classificação, que a equipe de suporte pode revisar.
            </p>
          </div>

          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <button type="submit" className="btn btn-primary" disabled={!valido}>
              Enviar chamado
            </button>
            <button
              type="button"
              className="btn btn-ghost"
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
  );
}
