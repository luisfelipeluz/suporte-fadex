/**
 * Contratos da API.
 *
 * Espelham os DTOs do backend (`br.org.fadex.chamados.web.dto`). Manter os tipos
 * em um único módulo faz o TypeScript apontar imediatamente qualquer divergência
 * entre o que a interface espera e o que a API devolve.
 */

export type Papel = 'ADMIN' | 'SOLICITANTE';

export type Prioridade = 'BAIXA' | 'MEDIA' | 'ALTA';

export type StatusChamado =
  | 'ABERTO'
  | 'EM_ANDAMENTO'
  | 'RESOLVIDO'
  | 'FECHADO'
  | 'CANCELADO';

export type Categoria =
  | 'HARDWARE'
  | 'SOFTWARE'
  | 'ACESSO'
  | 'REDE'
  | 'SISTEMAS'
  | 'OUTROS';

export type OrigemClassificacao = 'IA' | 'MANUAL';

export type Confianca = 'BAIXA' | 'MEDIA' | 'ALTA';

export type TipoEvento =
  | 'CHAMADO_ABERTO'
  | 'CLASSIFICACAO_IA'
  | 'CLASSIFICACAO_ACEITA'
  | 'CLASSIFICACAO_CORRIGIDA'
  | 'RESPONSAVEL_ATRIBUIDO'
  | 'STATUS_ALTERADO'
  | 'STATUS_RETROCEDIDO'
  | 'STATUS_RETROCEDIDO'
  | 'COMENTARIO_ADICIONADO'
  | 'CHAMADO_ATUALIZADO'
  | 'CHAMADO_CANCELADO';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
  papelRotulo: string;
  iniciais: string;
}

export interface Autenticacao {
  token: string;
  tipo: string;
  expiraEmSegundos: number;
  usuario: Usuario;
}

export interface Triagem {
  categoriaSugerida: Categoria | null;
  prioridadeSugerida: Prioridade | null;
  categoriaFinal: Categoria;
  prioridadeFinal: Prioridade;
  confianca: Confianca | null;
  confiancaPercentual: number | null;
  justificativa: string | null;
  origem: OrigemClassificacao;
  provedor: string | null;
  revisada: boolean;
  divergente: boolean;
}

export interface ChamadoResumo {
  id: number;
  titulo: string;
  categoria: Categoria;
  categoriaRotulo: string;
  prioridade: Prioridade;
  prioridadeRotulo: string;
  status: StatusChamado;
  statusRotulo: string;
  solicitante: Usuario;
  responsavel: Usuario | null;
  origemClassificacao: OrigemClassificacao;
  criadoEm: string;
  atualizadoEm: string;
}

export interface EventoHistorico {
  id: number;
  tipo: TipoEvento;
  descricao: string;
  etiqueta: string | null;
  cor: string;
  autor: string | null;
  criadoEm: string;
}

export interface Comentario {
  id: number;
  autor: string;
  iniciais: string;
  papel: Papel;
  texto: string;
  criadoEm: string;
}

export interface ChamadoSimilar {
  id: number;
  titulo: string;
  status: StatusChamado;
  statusRotulo: string;
  statusCor: string;
  solicitante: Usuario;
  criadoEm: string;
  /** Similaridade textual em pontos percentuais. */
  similaridade: number;
  termosEmComum: string[];
}

export interface ChamadoDetalhe extends Omit<ChamadoResumo, 'origemClassificacao'> {
  descricao: string;
  triagem: Triagem;
  proximoStatus: StatusChamado | null;
  /** Etapa anterior do fluxo, para onde o chamado pode retornar; nulo na primeira
   *  etapa e nos estados encerrados. */
  statusAnterior: StatusChamado | null;
  encerrado: boolean;
  historico: EventoHistorico[];
  comentarios: Comentario[];
  /** Chamados que possivelmente relatam o mesmo incidente. */
  possiveisDuplicados: ChamadoSimilar[];
}

export interface Pagina<T> {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
  primeira: boolean;
  ultima: boolean;
}

export interface Metricas {
  total: number;
  porStatus: Record<StatusChamado, number>;
  porPrioridade: Record<Prioridade, number>;
  porOrigem: Record<OrigemClassificacao, number>;
  /**
   * Quantidade por mecanismo que produziu a triagem (`heuristic`, `gemini`).
   *
   * Chave livre, e não um enum: o provider é definido por configuração no
   * backend, e um nome novo não deve exigir alteração de tipo aqui.
   */
  porProvedorTriagem: Record<string, number>;
  altaPrioridadeEmAberto: number;
  percentualClassificadoPorIa: number;
  atualizadoEm: string;
}

export interface Opcao {
  valor: string;
  rotulo: string;
  cor: string | null;
}

export interface FiltrosChamado {
  status?: StatusChamado | '';
  prioridade?: Prioridade | '';
  categoria?: Categoria | '';
  busca?: string;
  page?: number;
  size?: number;
}
