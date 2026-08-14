/**
 * Cliente HTTP da aplicação.
 *
 * Único ponto do frontend que fala com a rede. Nenhum componente chama `fetch`
 * diretamente: isso concentra em um lugar o token, o tratamento de erro e a
 * decisão sobre o que fazer quando a sessão expira.
 */

import type { ApiErro } from './erros';
import { ErroApi } from './erros';

const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

const CHAVE_TOKEN = 'fadex.token';

/** Callback disparado quando a API recusa o token (401). */
let aoExpirarSessao: (() => void) | null = null;

export function registrarExpiracaoDeSessao(callback: () => void) {
  aoExpirarSessao = callback;
}

export function lerToken(): string | null {
  return localStorage.getItem(CHAVE_TOKEN);
}

export function salvarToken(token: string) {
  localStorage.setItem(CHAVE_TOKEN, token);
}

export function limparToken() {
  localStorage.removeItem(CHAVE_TOKEN);
}

interface Opcoes {
  metodo?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  corpo?: unknown;
  /** Endpoints públicos (login/cadastro) não enviam token. */
  publico?: boolean;
}

export async function requisitar<T>(caminho: string, opcoes: Opcoes = {}): Promise<T> {
  const { metodo = 'GET', corpo, publico = false } = opcoes;

  const cabecalhos: Record<string, string> = {};
  if (corpo !== undefined) {
    cabecalhos['Content-Type'] = 'application/json';
  }

  if (!publico) {
    const token = lerToken();
    if (token) {
      cabecalhos.Authorization = `Bearer ${token}`;
    }
  }

  let resposta: Response;
  try {
    resposta = await fetch(`${BASE}${caminho}`, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
    });
  } catch {
    // Falha de rede: o servidor não respondeu.
    throw new ErroApi(
      0,
      'Não foi possível falar com o servidor. Verifique sua conexão e tente novamente.',
    );
  }

  if (resposta.status === 204) {
    return undefined as T;
  }

  const texto = await resposta.text();
  const dados = texto ? seguroJson(texto) : null;

  if (!resposta.ok) {
    const erro = dados as ApiErro | null;

    // Sessão expirada ou token inválido: derruba a sessão em vez de deixar a
    // interface tentando renderizar dados que nunca virão.
    if (resposta.status === 401 && !publico) {
      limparToken();
      aoExpirarSessao?.();
    }

    throw new ErroApi(
      resposta.status,
      erro?.mensagem ?? mensagemPadrao(resposta.status),
      erro?.campos ?? undefined,
    );
  }

  return dados as T;
}

function seguroJson(texto: string): unknown {
  try {
    return JSON.parse(texto);
  } catch {
    return null;
  }
}

function mensagemPadrao(status: number): string {
  switch (status) {
    case 400:
      return 'Há campos inválidos na requisição.';
    case 401:
      return 'Sua sessão expirou. Entre novamente.';
    case 403:
      return 'Você não possui permissão para executar esta ação.';
    case 404:
      return 'Recurso não encontrado.';
    case 409:
      return 'A operação não é permitida no estado atual.';
    default:
      return 'Ocorreu um erro inesperado. Tente novamente em alguns instantes.';
  }
}

/** Monta uma query string ignorando valores vazios. */
export function query(parametros: Record<string, string | number | undefined | null>): string {
  const partes = Object.entries(parametros)
    .filter(([, valor]) => valor !== undefined && valor !== null && valor !== '')
    .map(([chave, valor]) => `${encodeURIComponent(chave)}=${encodeURIComponent(String(valor))}`);

  return partes.length ? `?${partes.join('&')}` : '';
}
