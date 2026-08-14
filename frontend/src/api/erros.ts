/** Corpo de erro padronizado devolvido pela API (`ApiError` no backend). */
export interface ApiErro {
  timestamp: string;
  status: number;
  erro: string;
  mensagem: string;
  caminho: string;
  campos?: { campo: string; mensagem: string }[];
}

/**
 * Erro de API já traduzido para a interface.
 *
 * Carrega o status para que as telas possam distinguir "sem permissão" (403) de
 * "não encontrado" (404) e mostrar o estado visual correto, em vez de exibirem
 * sempre a mesma mensagem genérica.
 */
export class ErroApi extends Error {
  readonly status: number;
  readonly campos?: { campo: string; mensagem: string }[];

  constructor(status: number, mensagem: string, campos?: { campo: string; mensagem: string }[]) {
    super(mensagem);
    this.name = 'ErroApi';
    this.status = status;
    this.campos = campos;
  }

  /** Alias de `message`, para manter a nomenclatura do restante do código. */
  get mensagem(): string {
    return this.message;
  }

  get semPermissao(): boolean {
    return this.status === 403;
  }

  get naoEncontrado(): boolean {
    return this.status === 404;
  }

  get semConexao(): boolean {
    return this.status === 0;
  }

  /** Erros de validação por campo, indexados pelo nome do campo. */
  get porCampo(): Record<string, string> {
    const mapa: Record<string, string> = {};
    this.campos?.forEach((c) => {
      mapa[c.campo] = c.mensagem;
    });
    return mapa;
  }
}
