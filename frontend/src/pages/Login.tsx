/**
 * Entrada do sistema, em tela dividida.
 *
 * O painel escuro à esquerda não é enfeite: é o que apresenta o produto a quem
 * abre o sistema pela primeira vez — e, num desafio técnico, a quem vai avaliar.
 * O formulário fica à direita, com largura de leitura curta, para que a ação
 * principal não disponha o olho junto com o texto de apresentação.
 *
 * No celular a coluna escura sai de cena (`.login-split` em global.css): numa
 * tela estreita ela empurraria o formulário para baixo da dobra, que é
 * exatamente o oposto do que ela existe para fazer.
 */

import { useState, type FormEvent } from 'react';

import { ErroApi } from '../api/erros';
import { useAuth } from '../auth/AuthContext';
import { Icone } from '../components/Icone';
import { useTema } from '../styles/TemaContext';

type Aba = 'login' | 'cadastro';

/** Credenciais de demonstração, exibidas para agilizar a avaliação do desafio. */
const CONTAS_DEMO = [
  { papel: 'ADMIN', nome: 'Ana Souza', email: 'ana.souza@fadex.org.br', area: 'Suporte' },
  {
    papel: 'SOLICITANTE',
    nome: 'João Pereira',
    email: 'joao.pereira@fadex.org.br',
    area: 'Financeiro',
  },
] as const;

const SENHA_DEMO = 'suporte123';

/** Diferenciais listados no painel de apresentação, na ordem do design. */
const DESTAQUES = [
  'Gestão de chamados de ponta a ponta',
  'Triagem inteligente por IA, revisável pelo ADMIN',
  'Monitoramento em tempo real com alerta de alta prioridade',
];

export function Login() {
  const { entrar, cadastrar } = useAuth();
  const { efetivo, alternar } = useTema();

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
    fontSize: 13,
    fontWeight: 600,
    background: ativa ? 'var(--sf)' : 'transparent',
    color: ativa ? 'var(--ink)' : 'var(--mut)',
    boxShadow: ativa ? 'var(--sh1)' : 'none',
  });

  return (
    <div className="login-split">
      {/* ------------------------------------------------------------------ */}
      {/* Apresentação                                                        */}
      {/* ------------------------------------------------------------------ */}
      <aside className="login-marca">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span
            style={{
              width: 30,
              height: 30,
              background: 'var(--panel-ac)',
              borderRadius: 6,
              display: 'grid',
              placeItems: 'center',
              fontWeight: 800,
              fontSize: 14,
            }}
          >
            F
          </span>
          <span style={{ fontWeight: 700, fontSize: 15, letterSpacing: '-0.01em' }}>
            FADEX <span style={{ color: 'var(--panel-mut)', fontWeight: 500 }}>Suporte</span>
          </span>
        </div>

        <div style={{ maxWidth: 460, display: 'flex', flexDirection: 'column', gap: 28 }}>
          <p
            style={{
              margin: 0,
              fontSize: 13,
              fontWeight: 600,
              color: 'var(--panel-ac)',
              letterSpacing: '0.08em',
              textTransform: 'uppercase',
            }}
          >
            Central de Chamados
          </p>

          <h1
            style={{
              margin: 0,
              fontSize: 40,
              lineHeight: 1.08,
              fontWeight: 800,
              letterSpacing: '-0.03em',
              textWrap: 'pretty',
            }}
          >
            Abra o chamado. A triagem é automática.
          </h1>

          <p style={{ margin: 0, fontSize: 15, lineHeight: 1.6, color: 'var(--panel-mut)' }}>
            Descreva o problema em texto livre — a IA sugere categoria e prioridade, e a equipe de
            suporte acompanha tudo em tempo real.
          </p>

          <ol
            style={{
              listStyle: 'none',
              margin: 0,
              padding: '8px 0 0',
              display: 'flex',
              flexDirection: 'column',
              gap: 14,
            }}
          >
            {DESTAQUES.map((texto, i) => (
              <li key={texto} style={{ display: 'flex', gap: 12, alignItems: 'flex-start' }}>
                <span
                  aria-hidden="true"
                  style={{
                    width: 22,
                    height: 22,
                    flex: 'none',
                    border: '1px solid var(--panel-bd)',
                    borderRadius: 5,
                    display: 'grid',
                    placeItems: 'center',
                    color: 'var(--panel-ac)',
                    fontSize: 11,
                    fontWeight: 700,
                  }}
                >
                  {i + 1}
                </span>
                <span style={{ fontSize: 13.5, color: 'var(--panel-fg)', lineHeight: 1.5 }}>{texto}</span>
              </li>
            ))}
          </ol>
        </div>

        <p style={{ margin: 0, fontSize: 12, color: 'var(--panel-dim)' }}>
          Desafio técnico · FADEX · Central de Chamados
        </p>
      </aside>

      {/* ------------------------------------------------------------------ */}
      {/* Formulário                                                          */}
      {/* ------------------------------------------------------------------ */}
      <main className="login-form" style={{ position: 'relative' }}>
        {/* Fica na entrada tambem: quem prefere um tema nao deveria ter de
            autenticar antes de poder escolher. */}
        <button
          type="button"
          onClick={alternar}
          title={efetivo === 'escuro' ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'}
          aria-label={efetivo === 'escuro' ? 'Mudar para o tema claro' : 'Mudar para o tema escuro'}
          className="btn btn-secondary"
          style={{
            position: 'absolute',
            top: 20,
            right: 20,
            width: 34,
            height: 34,
            padding: 0,
            display: 'grid',
            placeItems: 'center',
          }}
        >
          <Icone nome={efetivo === 'escuro' ? 'sol' : 'lua'} tamanho={16} traco={1.9} />
        </button>

        <div style={{ width: '100%', maxWidth: 388 }}>
          <div
            role="tablist"
            style={{
              display: 'flex',
              gap: 2,
              background: 'var(--ntl)',
              padding: 3,
              borderRadius: 'var(--rd)',
              marginBottom: 26,
            }}
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

          <h2 style={{ margin: '0 0 6px', fontSize: 24, fontWeight: 700, letterSpacing: '-0.02em' }}>
            {aba === 'login' ? 'Central de Chamados' : 'Criar conta'}
          </h2>
          <p style={{ margin: '0 0 26px', fontSize: 14, color: 'var(--mut)' }}>
            {aba === 'login'
              ? 'Acesse com seu e-mail corporativo.'
              : 'Seu acesso é criado como solicitante e liberado pela equipe de suporte.'}
          </p>

          {erro && (
            <div
              role="alert"
              style={{
                display: 'flex',
                gap: 10,
                alignItems: 'flex-start',
                background: 'var(--hil)',
                border: '1px solid var(--hib)',
                borderRadius: 'var(--rd)',
                padding: '11px 12px',
                marginBottom: 18,
              }}
            >
              <Icone nome="alertaCirculo" tamanho={16} cor="var(--hi)" style={{ marginTop: 1 }} />
              <span style={{ fontSize: 13, color: 'var(--hif)', lineHeight: 1.45 }}>{erro}</span>
            </div>
          )}

          <form onSubmit={aoEnviar} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
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
                placeholder="nome@fadex.org.br"
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
              <>
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
                </div>

                <div
                  style={{
                    display: 'flex',
                    gap: 9,
                    alignItems: 'flex-start',
                    background: 'var(--ntl)',
                    borderRadius: 'var(--rd)',
                    padding: '10px 11px',
                  }}
                >
                  <Icone nome="info" tamanho={15} cor="var(--mut)" style={{ marginTop: 1 }} />
                  <span style={{ fontSize: 12, color: 'var(--mut)', lineHeight: 1.45 }}>
                    O papel de acesso não é escolhido no cadastro — apenas a equipe de suporte
                    concede o perfil ADMIN.
                  </span>
                </div>
              </>
            )}

            <button
              type="submit"
              className="btn btn-primary"
              disabled={carregando}
              style={{ marginTop: 4, width: '100%', padding: 12 }}
            >
              {carregando
                ? aba === 'login'
                  ? 'Entrando…'
                  : 'Criando conta…'
                : aba === 'login'
                  ? 'Entrar'
                  : 'Criar conta'}
            </button>
          </form>

          {/* Atalho para a avaliação do desafio */}
          <div style={{ marginTop: 30, paddingTop: 20, borderTop: '1px solid var(--bd)' }}>
            <p
              style={{
                margin: '0 0 10px',
                fontSize: 11,
                fontWeight: 700,
                letterSpacing: '0.07em',
                textTransform: 'uppercase',
                color: 'var(--mut)',
              }}
            >
              Demonstração — escolha o perfil
            </p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
              {CONTAS_DEMO.map((conta) => {
                const escolhida = email === conta.email;
                return (
                  <button
                    key={conta.email}
                    type="button"
                    onClick={() => usarConta(conta.email)}
                    style={{
                      textAlign: 'left',
                      padding: '10px 11px',
                      border: `1px solid ${escolhida ? 'var(--acb)' : 'var(--bd)'}`,
                      background: escolhida ? 'var(--acl)' : 'var(--sf)',
                      borderRadius: 'var(--rd)',
                      cursor: 'pointer',
                    }}
                  >
                    <span
                      style={{
                        display: 'block',
                        fontSize: 12.5,
                        fontWeight: 700,
                        color: 'var(--ink)',
                      }}
                    >
                      {conta.papel}
                    </span>
                    <span
                      style={{
                        display: 'block',
                        fontSize: 11.5,
                        color: 'var(--mut)',
                        lineHeight: 1.4,
                      }}
                    >
                      {conta.nome} · {conta.area}
                    </span>
                  </button>
                );
              })}
            </div>

            <p style={{ margin: '10px 0 0', fontSize: 11.5, color: 'var(--mut)' }}>
              Senha para ambas: <code className="mono">{SENHA_DEMO}</code>
            </p>
          </div>
        </div>
      </main>
    </div>
  );
}
