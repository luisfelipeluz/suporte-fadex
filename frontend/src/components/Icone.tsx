/**
 * Conjunto de ícones da interface.
 *
 * Os desenhos vêm do design de referência, que usa um único
 * sistema: grade de 24×24, traço sem preenchimento, pontas arredondadas. Ter
 * todos aqui — em vez de `<svg>` colado em cada tela — é o que mantém o
 * conjunto fechado: quem precisar de um ícone novo desenha nesta mesma grade,
 * e a interface não volta a misturar emoji com traço.
 *
 * Emoji foi o que existia antes, e o problema não era estético: emoji é
 * renderizado pela fonte do sistema operacional, então o mesmo ⚠️ aparecia
 * colorido no Windows, chapado no Linux e com outro desenho no celular — sem
 * respeitar a cor do texto ao redor. O traço acompanha `currentColor`.
 */

import type { CSSProperties, ReactElement } from 'react';

/** Ícones disponíveis. Os cinco últimos não existem no design de referência —
 *  foram desenhados na mesma grade para telas criadas depois dele. */
export type NomeIcone =
  | 'alerta'
  | 'alertaCirculo'
  | 'info'
  | 'sino'
  | 'chevronBaixo'
  | 'mais'
  | 'busca'
  | 'cadeado'
  | 'check'
  | 'brilho'
  | 'menu'
  | 'colunas'
  | 'duplicado'
  | 'setaEsquerda'
  | 'setaDireita'
  | 'sol'
  | 'lua';

/**
 * Traço de cada ícone, no viewBox 24×24 do design.
 *
 * `alerta` e `alertaCirculo` são o mesmo aviso em formas diferentes: o
 * triângulo aparece em estados de página inteira e no alerta de alta
 * prioridade, o círculo aparece embutido em formulário.
 */
const TRACOS: Record<NomeIcone, ReactElement> = {
  alerta: (
    <>
      <path d="M12 4l9 16H3z" />
      <line x1="12" y1="10" x2="12" y2="15" />
      <line x1="12" y1="17.5" x2="12" y2="17.5" />
    </>
  ),
  alertaCirculo: (
    <>
      <circle cx="12" cy="12" r="9" />
      <line x1="12" y1="8" x2="12" y2="13" />
      <line x1="12" y1="16" x2="12" y2="16" />
    </>
  ),
  info: (
    <>
      <circle cx="12" cy="12" r="9" />
      <line x1="12" y1="11" x2="12" y2="16" />
      <line x1="12" y1="8" x2="12" y2="8" />
    </>
  ),
  sino: (
    <>
      <path d="M6 9a6 6 0 0 1 12 0v5l2 3H4l2-3z" />
      <path d="M10 20a2 2 0 0 0 4 0" />
    </>
  ),
  chevronBaixo: <polyline points="6 9 12 15 18 9" />,
  mais: (
    <>
      <line x1="12" y1="5" x2="12" y2="19" />
      <line x1="5" y1="12" x2="19" y2="12" />
    </>
  ),
  busca: (
    <>
      <circle cx="11" cy="11" r="7" />
      <line x1="16.5" y1="16.5" x2="21" y2="21" />
    </>
  ),
  cadeado: (
    <>
      <rect x="5" y="11" width="14" height="9" />
      <path d="M8 11V8a4 4 0 0 1 8 0v3" />
    </>
  ),
  check: <polyline points="4 12 10 18 20 6" />,
  brilho: <path d="M12 3l1.9 5.4L19.5 10l-5.6 1.6L12 17l-1.9-5.4L4.5 10l5.6-1.6z" />,

  // --- Fora do design de referência, desenhados na mesma grade ---------------
  menu: (
    <>
      <line x1="4" y1="7" x2="20" y2="7" />
      <line x1="4" y1="12" x2="20" y2="12" />
      <line x1="4" y1="17" x2="20" y2="17" />
    </>
  ),
  colunas: (
    <>
      <rect x="3" y="5" width="18" height="14" />
      <line x1="9" y1="5" x2="9" y2="19" />
      <line x1="15" y1="5" x2="15" y2="19" />
    </>
  ),
  duplicado: (
    <>
      <rect x="9" y="9" width="11" height="11" />
      <path d="M15 9V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h3" />
    </>
  ),
  setaEsquerda: (
    <>
      <line x1="19" y1="12" x2="5" y2="12" />
      <polyline points="11 6 5 12 11 18" />
    </>
  ),
  setaDireita: (
    <>
      <line x1="5" y1="12" x2="19" y2="12" />
      <polyline points="13 6 19 12 13 18" />
    </>
  ),
  sol: (
    <>
      <circle cx="12" cy="12" r="4.2" />
      <line x1="12" y1="2.5" x2="12" y2="5" />
      <line x1="12" y1="19" x2="12" y2="21.5" />
      <line x1="2.5" y1="12" x2="5" y2="12" />
      <line x1="19" y1="12" x2="21.5" y2="12" />
      <line x1="5.6" y1="5.6" x2="7.4" y2="7.4" />
      <line x1="16.6" y1="16.6" x2="18.4" y2="18.4" />
      <line x1="18.4" y1="5.6" x2="16.6" y2="7.4" />
      <line x1="7.4" y1="16.6" x2="5.6" y2="18.4" />
    </>
  ),
  lua: <path d="M20 14.5A8.5 8.5 0 0 1 9.5 4a8.5 8.5 0 1 0 10.5 10.5z" />,
};

export function Icone({
  nome,
  tamanho = 16,
  traco = 2,
  cor = 'currentColor',
  style,
}: {
  nome: NomeIcone;
  /** Lado do quadrado, em px. O design usa de 13 a 26. */
  tamanho?: number;
  /** Espessura do traço. O design varia entre 1.7 e 2.4 conforme o tamanho. */
  traco?: number;
  cor?: string;
  style?: CSSProperties;
}) {
  return (
    <svg
      // Decorativo por padrão: o significado sempre vem do texto ao lado ou de
      // um aria-label no controle que envolve o ícone.
      aria-hidden="true"
      focusable="false"
      width={tamanho}
      height={tamanho}
      viewBox="0 0 24 24"
      fill="none"
      stroke={cor}
      strokeWidth={traco}
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ flex: 'none', ...style }}
    >
      {TRACOS[nome]}
    </svg>
  );
}

/**
 * Ícone dentro de um círculo, como nos estados de página inteira do design
 * (44px de círculo, ícone de 20 a 21px).
 */
export function IconeCirculo({
  nome,
  fundo,
  cor,
  tamanho = 44,
}: {
  nome: NomeIcone;
  fundo: string;
  cor: string;
  tamanho?: number;
}) {
  return (
    <div
      aria-hidden="true"
      style={{
        width: tamanho,
        height: tamanho,
        borderRadius: '50%',
        background: fundo,
        display: 'grid',
        placeItems: 'center',
        flex: 'none',
      }}
    >
      <Icone nome={nome} tamanho={Math.round(tamanho * 0.47)} cor={cor} />
    </div>
  );
}
