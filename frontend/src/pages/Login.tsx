import { useState, type FormEvent } from 'react';

import { ErroApi } from '../api/erros';
import { useAuth } from '../auth/AuthContext';

type Aba = 'login' | 'cadastro';

/** Credenciais de demonstração, exibidas para agilizar a avaliação do desafio. */
const CONTAS_DEMO = [
  { papel: 'ADMIN', nome: 'Ana Souza', email: 'ana.souza@fadex.org.br' },
  { papel: 'SOLICITANTE', nome: 'João Pereira', email: 'joao.pereira@fadex.org.br' },
] as const;

const SENHA_DEMO = 'suporte123';

export function Login() {
  const { entrar, cadastrar } = useAuth();

  const [aba, setAba] = useState<Aba>('login');
  const [email, setEmail] = useState<string>(CONTAS_DEMO[0].email);
  const [senha, setSenha] = useState<string>(SENHA_DEMO);
  const [nome, setNome] = useState('');
  const [confirmacao, setConfirmacao] = useState('');

  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [errosCampo, setErrosCampo] = useState<Record<string, string>>({});

  function limparErros() {
    setErro(null);
    setErrosCampo({});
  }

  async function aoEnviar(e: FormEvent) {
    e.preventDefault();
    limparErros();

    if (aba === 'cadastro' && senha !== confirmacao) {
      setErrosCampo({ confirmacao: 'As senhas não conferem.' });
      return;
    }

    setCarregando(true);
    try {
      if (aba === 'login') {
        await entrar(email, senha);
      } else {
        await cadastrar(nome, email, senha);
      }
    } catch (e) {
      if (e instanceof ErroApi) {
        setErro(e.mensagem);
        setErrosCampo(e.porCampo);
      } else {
        setErro('Não foi possível concluir. Tente novamente.');
      }
    } finally {
      setCarregando(false);
    }
  }

  function usarConta(emailDemo: string) {
    setAba('login');
    setEmail(emailDemo);
    setSenha(SENHA_DEMO);
    limparErros();
  }

  const abaEstilo = (ativa: boolean) => ({
    flex: 1,
    padding: '9px 12px',
    borderRadius: 6,
    border: 'none',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 600,
    background: ativa ? '#fff' : 'transparent',
    color: ativa ? 'var(--ink)' : 'var(--mut)',
    boxShadow: ativa ? '0 1px 2px rgba(15,23,42,.1)' : 'none',
  });

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'grid',
        gridTemplateColumns: 'minmax(0, 1fr)',
        placeItems: 'center',
        padding: 24,
      }}
    >
      <div style={{ width: '100%', maxWidth: 420 }}>
        {/* Identidade */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <div
            style={{
              width: 46,
              height: 46,
              borderRadius: 12,
              background: 'var(--ac)',
              color: '#fff',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 22,
              fontWeight: 800,
              marginBottom: 12,
            }}
          >
            F
          </div>
          <h1 style={{ margin: 0, fontSize: 24, letterSpacing: '-0.02em' }}>Central de Chamados</h1>
          <p style={{ margin: '6px 0 0', fontSize: 14, color: 'var(--mut)' }}>
            Suporte interno com triagem inteligente
          </p>
        </div>

        <div className="card card-pad">
          {/* Alternância login / cadastro */}
          <div
            role="tablist"
            style={{ display: 'flex', gap: 4, background: 'var(--ntl)', padding: 4, borderRadius: 8, marginBottom: 20 }}
          >
            <button
              type="button"
              role="tab"
              aria-selected={aba === 'login'}
              style={abaEstilo(aba === 'login')}
              onClick={() => {
                setAba('login');
                limparErros();
              }}
            >
              Entrar
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={aba === 'cadastro'}
              style={abaEstilo(aba === 'cadastro')}
              onClick={() => {
                setAba('cadastro');
                limparErros();
              }}
            >
              Criar conta
            </button>
          </div>

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

          <form onSubmit={aoEnviar} style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {aba === 'cadastro' && (
              <div className="field">
                <label className="label" htmlFor="nome">
                  Nome completo
                </label>
                <input
                  id="nome"
                  className={`input ${errosCampo.nome ? 'input-erro' : ''}`}
                  value={nome}
                  onChange={(e) => setNome(e.target.value)}
                  placeholder="Como você aparece para a equipe"
                  autoComplete="name"
                  required
                />
                {errosCampo.nome && <span className="erro-campo">{errosCampo.nome}</span>}
              </div>
            )}

            <div className="field">
              <label className="label" htmlFor="email">
                E-mail
              </label>
              <input
                id="email"
                type="email"
                className={`input ${errosCampo.email ? 'input-erro' : ''}`}
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="seu.nome@fadex.org.br"
                autoComplete="email"
                required
              />
              {errosCampo.email && <span className="erro-campo">{errosCampo.email}</span>}
            </div>

            <div className="field">
              <label className="label" htmlFor="senha">
                Senha
              </label>
              <input
                id="senha"
                type="password"
                className={`input ${errosCampo.senha ? 'input-erro' : ''}`}
                value={senha}
                onChange={(e) => setSenha(e.target.value)}
                autoComplete={aba === 'login' ? 'current-password' : 'new-password'}
                required
              />
              {errosCampo.senha && <span className="erro-campo">{errosCampo.senha}</span>}
            </div>

            {aba === 'cadastro' && (
              <div className="field">
                <label className="label" htmlFor="confirmacao">
                  Confirmar senha
                </label>
                <input
                  id="confirmacao"
                  type="password"
                  className={`input ${errosCampo.confirmacao ? 'input-erro' : ''}`}
                  value={confirmacao}
                  onChange={(e) => setConfirmacao(e.target.value)}
                  autoComplete="new-password"
                  required
                />
                {errosCampo.confirmacao && (
                  <span className="erro-campo">{errosCampo.confirmacao}</span>
                )}
                <span className="hint">
                  Contas criadas aqui recebem o perfil de solicitante.
                </span>
              </div>
            )}

            <button type="submit" className="btn btn-primary" disabled={carregando} style={{ marginTop: 4 }}>
              {carregando
                ? aba === 'login'
                  ? 'Entrando…'
                  : 'Criando conta…'
                : aba === 'login'
                  ? 'Entrar'
                  : 'Criar conta'}
            </button>
          </form>
        </div>

        {/* Atalho para a avaliação do desafio */}
        <div className="card" style={{ marginTop: 16, padding: 16 }}>
          <p style={{ margin: '0 0 10px', fontSize: 12, fontWeight: 700, color: 'var(--mut)' }}>
            CONTAS DE TESTE
          </p>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {CONTAS_DEMO.map((conta) => (
              <button
                key={conta.email}
                type="button"
                onClick={() => usarConta(conta.email)}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  gap: 12,
                  padding: '9px 12px',
                  border: '1px solid var(--bd)',
                  borderRadius: 8,
                  background: 'var(--sf)',
                  cursor: 'pointer',
                  textAlign: 'left',
                }}
              >
                <span>
                  <span style={{ display: 'block', fontSize: 13, fontWeight: 700 }}>{conta.nome}</span>
                  <span style={{ display: 'block', fontSize: 12, color: 'var(--mut)' }}>
                    {conta.email}
                  </span>
                </span>
                <span
                  className="badge"
                  style={{
                    background: conta.papel === 'ADMIN' ? 'var(--acl)' : 'var(--ntl)',
                    color: conta.papel === 'ADMIN' ? 'var(--acd)' : 'var(--ink2)',
                  }}
                >
                  {conta.papel}
                </span>
              </button>
            ))}
          </div>
          <p style={{ margin: '10px 0 0', fontSize: 12, color: 'var(--mut)' }}>
            Senha para ambas: <code className="mono">{SENHA_DEMO}</code>
          </p>
        </div>
      </div>
    </div>
  );
}
