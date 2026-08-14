import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/AuthContext';
import { Avatar } from '../components/Badges';
import { Icone } from '../components/Icone';
import { useRealtime, type EstadoConexao } from '../realtime/RealtimeContext';
import { horaDe } from '../utils/formato';
import { AlertaAltaPrioridade } from './AlertaAltaPrioridade';
import { CentralNotificacoes } from './CentralNotificacoes';

const CORES_CONEXAO: Record<EstadoConexao, { cor: string; rotulo: string }> = {
  conectado: { cor: '#22c55e', rotulo: 'Conectado' },
  conectando: { cor: '#f59e0b', rotulo: 'Conectando…' },
  reconectando: { cor: '#f59e0b', rotulo: 'Reconectando…' },
};

/**
 * Titulo da tela atual, exibido no cabecalho.
 *
 * O detalhe do chamado nao entra no mapa porque o shell nao conhece o titulo
 * do chamado aberto — quem sabe disso e a propria tela, que ja o mostra.
 */
function tituloDaRota(pathname: string, admin: boolean): string {
  if (pathname.startsWith('/chamados/novo')) return 'Novo chamado';
  if (/^\/chamados\/\d+/.test(pathname)) return 'Chamado';
  if (pathname.startsWith('/chamados')) return admin ? 'Chamados' : 'Meus chamados';
  if (pathname.startsWith('/dashboard')) return 'Visão geral';
  return 'Central de Chamados';
}

export function AppShell() {
  const { usuario, admin, sair } = useAuth();
  const { conexao, metricas } = useRealtime();
  const navegar = useNavigate();
  const local = useLocation();

  const [menuUsuario, setMenuUsuario] = useState(false);
  const [menuMobile, setMenuMobile] = useState(false);

  // Fecha os menus ao navegar, para que não fiquem abertos sobre a nova tela.
  useEffect(() => {
    setMenuUsuario(false);
    setMenuMobile(false);
  }, [local.pathname]);

  if (!usuario) return null;

  const abertos = metricas?.porStatus.ABERTO ?? 0;

  // A abertura de chamado nao entra no menu: a listagem ja traz o botao, e ele
  // fica ao lado da fila que a pessoa acabou de olhar — que e onde a vontade de
  // abrir um chamado costuma aparecer. A rota continua existindo.
  const itens = admin
    ? [
        { para: '/dashboard', rotulo: 'Dashboard', contador: null },
        { para: '/chamados', rotulo: 'Chamados', contador: abertos },
      ]
    : [{ para: '/chamados', rotulo: 'Meus chamados', contador: abertos }];

  const conexaoAtual = CORES_CONEXAO[conexao];

  return (
    <div style={{ minHeight: '100vh', display: 'flex', background: 'var(--bg)' }}>
      {/* ----------------------------------------------------------------- */}
      {/* Sidebar                                                            */}
      {/* ----------------------------------------------------------------- */}
      <aside
        style={{
          width: 232,
          background: '#0f172a',
          padding: '20px 14px',
          display: 'flex',
          flexDirection: 'column',
          gap: 24,
          position: 'fixed',
          insetBlock: 0,
          left: 0,
          zIndex: 50,
        }}
        className={menuMobile ? 'sidebar sidebar-aberta' : 'sidebar'}
      >
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            padding: '0 8px 16px',
            borderBottom: '1px solid #1e293b',
          }}
        >
          <span
            style={{
              width: 28,
              height: 28,
              borderRadius: 6,
              background: 'var(--ac)',
              color: '#fff',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
              fontSize: 13,
            }}
          >
            F
          </span>
          <span
            style={{ color: '#fff', fontWeight: 700, fontSize: 14, letterSpacing: '-0.01em' }}
          >
            FADEX <span style={{ color: '#94a3b8', fontWeight: 500 }}>Suporte</span>
          </span>
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
          {itens.map((item) => (
            <NavLink
              key={item.para}
              to={item.para}
              end={item.para === '/chamados'}
              style={({ isActive }) => ({
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '9px 11px',
                borderRadius: 6,
                fontSize: 13.5,
                fontWeight: isActive ? 700 : 500,
                background: isActive ? 'var(--ac)' : 'transparent',
                color: isActive ? '#fff' : '#cbd5e1',
                textDecoration: 'none',
              })}
            >
              {({ isActive }) => (
                <>
                  <span
                    aria-hidden="true"
                    style={{
                      width: 6,
                      height: 6,
                      borderRadius: '50%',
                      background: isActive ? '#fff' : '#475569',
                      flex: 'none',
                    }}
                  />
                  <span style={{ flex: 1 }}>{item.rotulo}</span>
                  {item.contador != null && item.contador > 0 && (
                    <span
                      title={`${item.contador} em aberto`}
                      style={{
                        fontSize: 11,
                        fontWeight: 700,
                        padding: '1px 6px',
                        borderRadius: 20,
                        background: isActive ? 'rgba(255,255,255,.25)' : '#1e293b',
                        color: isActive ? '#fff' : '#94a3b8',
                      }}
                    >
                      {item.contador}
                    </span>
                  )}
                </>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Indicador de tempo real */}
        <div style={{ marginTop: 'auto', padding: '12px', borderTop: '1px solid #1e293b' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <span
              aria-hidden="true"
              style={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                background: conexaoAtual.cor,
                animation: conexao === 'conectado' ? 'pls 2.4s infinite' : 'none',
              }}
            />
            <span style={{ fontSize: 12, color: '#94a3b8' }}>{conexaoAtual.rotulo}</span>
          </div>
          <p style={{ margin: '6px 0 0', fontSize: 11, color: '#64748b', lineHeight: 1.45 }}>
            {metricas
              ? `Fila sincronizada · última atualização ${horaDe(metricas.atualizadoEm)}`
              : 'Atualização em tempo real'}
          </p>
        </div>
      </aside>

      {menuMobile && (
        <div
          onClick={() => setMenuMobile(false)}
          style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,.45)', zIndex: 40 }}
        />
      )}

      {/* ----------------------------------------------------------------- */}
      {/* Conteúdo                                                           */}
      {/* ----------------------------------------------------------------- */}
      <div style={{ flex: 1, marginLeft: 232, minWidth: 0 }} className="conteudo">
        <header
          style={{
            height: 60,
            background: 'rgba(255,255,255,.92)',
            backdropFilter: 'blur(6px)',
            borderBottom: '1px solid var(--bd)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 20px',
            gap: 16,
            position: 'sticky',
            top: 0,
            zIndex: 30,
          }}
        >
          <button
            type="button"
            className="btn btn-ghost btn-sm abrir-menu"
            onClick={() => setMenuMobile(true)}
            aria-label="Abrir menu"
          >
            <Icone nome="menu" tamanho={18} />
          </button>

          <h1 style={{ margin: 0, fontSize: 14.5, fontWeight: 700, letterSpacing: '-0.01em' }}>
            {tituloDaRota(local.pathname, admin)}
          </h1>

          <div style={{ flex: 1 }} />

          <CentralNotificacoes />

          {/* Menu do usuário */}
          <div style={{ position: 'relative' }}>
            <button
              type="button"
              onClick={() => setMenuUsuario((v) => !v)}
              aria-expanded={menuUsuario}
              aria-haspopup="menu"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '5px 8px',
                border: '1px solid var(--bd)',
                borderRadius: 999,
                background: 'var(--sf)',
                cursor: 'pointer',
              }}
            >
              <Avatar iniciais={usuario.iniciais} destaque={!admin} tamanho={28} />
              <span style={{ textAlign: 'left', lineHeight: 1.2 }}>
                <span style={{ display: 'block', fontSize: 13, fontWeight: 700 }}>
                  {usuario.nome}
                </span>
                <span
                  style={{
                    display: 'block',
                    fontSize: 11,
                    color: admin ? 'var(--acd)' : 'var(--mut)',
                    fontWeight: 600,
                  }}
                >
                  {usuario.papel}
                </span>
              </span>
              <Icone nome="chevronBaixo" tamanho={14} cor="var(--mut)" />
            </button>

            {menuUsuario && (
              <div
                role="menu"
                className="card"
                style={{
                  position: 'absolute',
                  right: 0,
                  top: 'calc(100% + 8px)',
                  width: 220,
                  padding: 8,
                  boxShadow: 'var(--sh2)',
                  zIndex: 40,
                }}
              >
                <div style={{ padding: '8px 10px', borderBottom: '1px solid var(--bd)' }}>
                  <p style={{ margin: 0, fontSize: 13, fontWeight: 700 }}>{usuario.nome}</p>
                  <p style={{ margin: 0, fontSize: 12, color: 'var(--mut)' }}>{usuario.email}</p>
                </div>
                <button
                  type="button"
                  role="menuitem"
                  className="btn btn-ghost"
                  style={{ width: '100%', justifyContent: 'flex-start', marginTop: 4 }}
                  onClick={() => {
                    sair();
                    navegar('/');
                  }}
                >
                  Sair
                </button>
              </div>
            )}
          </div>
        </header>

        <main style={{ padding: 24, maxWidth: 1280 }}>
          <Outlet />
        </main>
      </div>

      <AlertaAltaPrioridade />

      <style>{`
        .abrir-menu { display: none; }

        /* No mobile a sidebar vira menu deslizante e o conteúdo ocupa a tela. */
        @media (max-width: 900px) {
          .sidebar {
            transform: translateX(-100%);
            transition: transform .2s ease;
          }
          .sidebar-aberta { transform: translateX(0); }
          .conteudo { margin-left: 0 !important; }
          .abrir-menu { display: inline-flex; }
        }
      `}</style>
    </div>
  );
}
