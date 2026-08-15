/**
 * Estados de interface: carregando, vazio, erro, sem permissão e não encontrado.
 *
 * Centralizá-los aqui garante que toda tela trate as mesmas situações do mesmo
 * jeito — e que nenhuma delas mostre uma tela em branco quando algo dá errado.
 */

import type { ReactNode } from 'react';
import { ErroApi } from '../api/erros';
import { IconeCirculo, type NomeIcone } from './Icone';

function Moldura({
  icone,
  fundo,
  cor,
  titulo,
  descricao,
  acao,
}: {
  icone: NomeIcone;
  /** Cor do círculo atrás do ícone: neutra para estados sem falha, vermelha
   *  quando algo deu errado — como no design. */
  fundo: string;
  cor: string;
  titulo: string;
  descricao: string;
  acao?: ReactNode;
}) {
  return (
    <div
      style={{
        padding: '48px 24px',
        textAlign: 'center',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 8,
      }}
    >
      <IconeCirculo nome={icone} fundo={fundo} cor={cor} />
      <p style={{ margin: 0, fontWeight: 700, color: 'var(--ink)' }}>{titulo}</p>
      <p style={{ margin: 0, fontSize: 14, color: 'var(--mut)', maxWidth: 420 }}>{descricao}</p>
      {acao && <div style={{ marginTop: 8 }}>{acao}</div>}
    </div>
  );
}

export function EstadoVazio({
  titulo = 'Nenhum chamado por aqui',
  descricao = 'Quando houver chamados que atendam a estes critérios, eles aparecerão nesta lista.',
  acao,
}: {
  titulo?: string;
  descricao?: string;
  acao?: ReactNode;
}) {
  return (
    <Moldura
      icone="busca"
      fundo="var(--ntl)"
      cor="var(--dim)"
      titulo={titulo}
      descricao={descricao}
      acao={acao}
    />
  );
}

export function EstadoSemPermissao() {
  return (
    <Moldura
      icone="cadeado"
      fundo="var(--hil)"
      cor="var(--hi)"
      titulo="Você não possui permissão para visualizar este chamado"
      descricao="Este chamado pertence a outro solicitante. Se acredita que isso é um engano, procure a equipe de suporte."
    />
  );
}

export function EstadoNaoEncontrado() {
  return (
    <Moldura
      icone="busca"
      fundo="var(--ntl)"
      cor="var(--dim)"
      titulo="Chamado não encontrado"
      descricao="O chamado que você tentou abrir não existe ou foi removido."
    />
  );
}

export function EstadoErro({ erro, aoTentarNovamente }: { erro: unknown; aoTentarNovamente?: () => void }) {
  const mensagem =
    erro instanceof ErroApi
      ? erro.mensagem
      : 'Não foi possível carregar as informações. Tente novamente.';

  const semConexao = erro instanceof ErroApi && erro.semConexao;

  return (
    <Moldura
      icone="alerta"
      fundo="var(--hil)"
      cor="var(--hi)"
      titulo={semConexao ? 'Sem conexão com o servidor' : 'Não foi possível carregar'}
      descricao={mensagem}
      acao={
        aoTentarNovamente && (
          <button type="button" className="btn btn-secondary" onClick={aoTentarNovamente}>
            Tentar novamente
          </button>
        )
      }
    />
  );
}

/** Esqueleto de linhas, usado enquanto a listagem carrega. */
export function EsqueletoLinhas({ linhas = 6 }: { linhas?: number }) {
  return (
    <div style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
      {Array.from({ length: linhas }, (_, i) => (
        <div key={i} style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div className="skeleton" style={{ width: 48, height: 14 }} />
          <div className="skeleton" style={{ flex: 1, height: 14 }} />
          <div className="skeleton" style={{ width: 90, height: 20, borderRadius: 999 }} />
          <div className="skeleton" style={{ width: 110, height: 20, borderRadius: 999 }} />
          <div className="skeleton" style={{ width: 80, height: 14 }} />
        </div>
      ))}
      <span className="sr-only">Carregando chamados…</span>
    </div>
  );
}

export function EstadoCarregando({ texto = 'Carregando…' }: { texto?: string }) {
  return (
    <div
      style={{
        padding: 48,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        gap: 12,
        color: 'var(--mut)',
      }}
    >
      <span
        aria-hidden="true"
        style={{
          width: 22,
          height: 22,
          border: '2px solid var(--bd)',
          borderTopColor: 'var(--ac)',
          borderRadius: '50%',
          animation: 'spin .7s linear infinite',
        }}
      />
      <span style={{ fontSize: 14 }}>{texto}</span>
    </div>
  );
}
