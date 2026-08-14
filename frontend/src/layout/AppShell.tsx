import { useEffect, useState } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';

import { useAuth } from '../auth/AuthContext';
import { Avatar } from '../components/Badges';
import { Icone } from '../components/Icone';
import { useRealtime, type EstadoConexao } from '../realtime/RealtimeContext';
import { AlertaAltaPrioridade } from './AlertaAltaPrioridade';
import { CentralNotificacoes } from './CentralNotificacoes';

const CORES_CONEXAO: Record<EstadoConexao, { cor: string; rotulo: string }> = {
  conectado: { cor: '#22c55e', rotulo: 'Conectado' },
  conectando: { cor: '#f59e0b', rotulo: 'Conectando…' },
  reconectando: { cor: '#f59e0b', rotulo: 'Reconectando…' },
};

export function AppShell() {
  const { usuario, admin, sair } = useAuth();
  const { conexao } = useRealtime();
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

  const itens = admin
    ? [
        { para: '/dashboard', rotulo: 'Dashboard' },
        { para: '/chamados', rotulo: 'Chamados' },
        { para: '/chamados/novo', rotulo: 'Novo chamado' },
      ]
    : [
        { para: '/chamados', rotulo: 'Meus chamados' },
        { para: '/chamados/novo', rotulo: 'Novo chamado' },
      ];

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
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '0 8px' }}>
          <span
            style={{
              width: 32,
              height: 32,
              borderRadius: 8,
              background: 'var(--ac)',
              color: '#fff',
              display: 'inline-flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontWeight: 800,
            }}
          >
            F
          </span>
          <span style={{ color: '#fff', fontWeight: 700, fontSize: 14, lineHeight: 1.2 }}>
            Central de
            <br />
            Chamados
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
                padding: '10px 12px',
                borderRadius: 8,
                fontSize: 14,
                fontWeight: isActive ? 700 : 500,
                background: isActive ? 'var(--ac)' : 'transparent',
                color: isActive ? '#fff' : '#cbd5e1',
                textDecoration: 'none',
              })}
            >
              {item.rotulo}
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
          <p style={{ margin: '6px 0 0', fontSize: 11, color: '#64748b' }}>
            Atualização em tempo real
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
            background: 'var(--sf)',
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
