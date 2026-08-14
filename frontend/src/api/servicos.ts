/**
 * Serviços da API, organizados por recurso.
 *
 * As telas importam estas funções e nunca conhecem caminhos, verbos ou formato
 * de query string — trocar a rota de um endpoint é uma alteração local aqui.
 */

import { query, requisitar } from './client';
import type {
  Autenticacao,
  ChamadoDetalhe,
  ChamadoResumo,
  Categoria,
  Comentario,
  FiltrosChamado,
  Metricas,
  Opcao,
  Pagina,
  Prioridade,
  StatusChamado,
  Usuario,
} from './tipos';

// --- Autenticação -----------------------------------------------------------

export const autenticacao = {
  login: (email: string, senha: string) =>
    requisitar<Autenticacao>('/api/auth/login', {
      metodo: 'POST',
      corpo: { email, senha },
      publico: true,
    }),

  registrar: (nome: string, email: string, senha: string) =>
    requisitar<Autenticacao>('/api/auth/registrar', {
      metodo: 'POST',
      corpo: { nome, email, senha },
      publico: true,
    }),

  /** Restaura a sessão a partir de um token guardado no navegador. */
  eu: () => requisitar<Usuario>('/api/auth/eu'),
};

// --- Chamados ---------------------------------------------------------------

export const chamados = {
  listar: (filtros: FiltrosChamado = {}) =>
    requisitar<Pagina<ChamadoResumo>>(
      `/api/chamados${query({
        status: filtros.status,
        prioridade: filtros.prioridade,
        categoria: filtros.categoria,
        busca: filtros.busca,
        page: filtros.page,
        size: filtros.size,
      })}`,
    ),

  detalhar: (id: number) => requisitar<ChamadoDetalhe>(`/api/chamados/${id}`),

  criar: (titulo: string, descricao: string) =>
    requisitar<ChamadoDetalhe>('/api/chamados', {
      metodo: 'POST',
      corpo: { titulo, descricao },
    }),

  atualizar: (id: number, titulo: string, descricao: string) =>
    requisitar<ChamadoDetalhe>(`/api/chamados/${id}`, {
      metodo: 'PUT',
      corpo: { titulo, descricao },
    }),

  cancelar: (id: number) => requisitar<void>(`/api/chamados/${id}`, { metodo: 'DELETE' }),

  alterarStatus: (id: number, status: StatusChamado) =>
    requisitar<ChamadoDetalhe>(`/api/chamados/${id}/status`, {
      metodo: 'PATCH',
      corpo: { status },
    }),

  atribuirResponsavel: (id: number, responsavelId: number) =>
    requisitar<ChamadoDetalhe>(`/api/chamados/${id}/responsavel`, {
      metodo: 'PATCH',
      corpo: { responsavelId },
    }),

  aceitarTriagem: (id: number) =>
    requisitar<ChamadoDetalhe>(`/api/chamados/${id}/triagem/aceitar`, { metodo: 'POST' }),

  corrigirTriagem: (id: number, categoria: Categoria, prioridade: Prioridade) =>
    requisitar<ChamadoDetalhe>(`/api/chamados/${id}/triagem`, {
      metodo: 'PATCH',
      corpo: { categoria, prioridade },
    }),
};

// --- Comentários ------------------------------------------------------------

export const comentarios = {
  listar: (chamadoId: number) =>
    requisitar<Comentario[]>(`/api/chamados/${chamadoId}/comentarios`),

  adicionar: (chamadoId: number, texto: string) =>
    requisitar<Comentario>(`/api/chamados/${chamadoId}/comentarios`, {
      metodo: 'POST',
      corpo: { texto },
    }),
};

// --- Indicadores ------------------------------------------------------------

export const dashboard = {
  metricas: () => requisitar<Metricas>('/api/dashboard/metricas'),
};

// --- Listas de referência ---------------------------------------------------

export const referencias = {
  categorias: () => requisitar<Opcao[]>('/api/categorias'),
  prioridades: () => requisitar<Opcao[]>('/api/prioridades'),
  status: () => requisitar<Opcao[]>('/api/status'),
  responsaveis: () => requisitar<Usuario[]>('/api/responsaveis'),
};
