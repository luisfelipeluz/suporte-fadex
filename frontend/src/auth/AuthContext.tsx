import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react';

import {
  limparToken,
  registrarExpiracaoDeSessao,
  salvarToken,
} from '../api/client';
import { autenticacao } from '../api/servicos';
import type { Usuario } from '../api/tipos';

interface EstadoAuth {
  usuario: Usuario | null;
  /** Verdadeiro enquanto a sessão é restaurada a partir do token salvo. */
  carregando: boolean;
  admin: boolean;
  entrar: (email: string, senha: string) => Promise<void>;
  cadastrar: (nome: string, email: string, senha: string) => Promise<void>;
  sair: () => void;
}

const AuthContext = createContext<EstadoAuth | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<Usuario | null>(null);
  const [carregando, setCarregando] = useState(true);

  const sair = useCallback(() => {
    limparToken();
    setUsuario(null);
  }, []);

  // Um 401 vindo de qualquer requisição derruba a sessão, em vez de deixar a
  // interface presa em telas que nunca vão carregar.
  useEffect(() => {
    registrarExpiracaoDeSessao(() => setUsuario(null));
  }, []);

  // Recupera a sessão ao abrir a aplicação: o token sobrevive ao refresh, mas
  // quem confirma se ele ainda vale é o servidor.
  useEffect(() => {
    let ativo = true;

    autenticacao
      .eu()
      .then((u) => {
        if (ativo) setUsuario(u);
      })
      .catch(() => {
        if (ativo) setUsuario(null);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, []);

  const entrar = useCallback(async (email: string, senha: string) => {
    const resposta = await autenticacao.login(email, senha);
    salvarToken(resposta.token);
    setUsuario(resposta.usuario);
  }, []);

  const cadastrar = useCallback(async (nome: string, email: string, senha: string) => {
    const resposta = await autenticacao.registrar(nome, email, senha);
    salvarToken(resposta.token);
    setUsuario(resposta.usuario);
  }, []);

  const valor = useMemo<EstadoAuth>(
    () => ({
      usuario,
      carregando,
      admin: usuario?.papel === 'ADMIN',
      entrar,
      cadastrar,
      sair,
    }),
    [usuario, carregando, entrar, cadastrar, sair],
  );

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>;
}

export function useAuth(): EstadoAuth {
  const contexto = useContext(AuthContext);
  if (!contexto) {
    throw new Error('useAuth precisa estar dentro de <AuthProvider>.');
  }
  return contexto;
}
