import { Navigate, Route, BrowserRouter as Router, Routes } from 'react-router-dom';

import { AuthProvider, useAuth } from './auth/AuthContext';
import { EstadoCarregando } from './components/Estados';
import { ToastProvider } from './components/Toasts';
import { AppShell } from './layout/AppShell';
import { Dashboard } from './pages/Dashboard';
import { DetalheChamado } from './pages/DetalheChamado';
import { ListaChamados } from './pages/ListaChamados';
import { Login } from './pages/Login';
import { NovoChamado } from './pages/NovoChamado';
import { RealtimeProvider } from './realtime/RealtimeContext';

/**
 * Área autenticada.
 *
 * Enquanto a sessão é restaurada mostra um estado de carregamento — sem isso a
 * tela de login pisca a cada refresh antes do token ser validado.
 *
 * Esta guarda é de experiência, não de segurança: quem protege os dados é o
 * backend, que recusa qualquer requisição sem token válido.
 */
function AreaAutenticada() {
  const { usuario, carregando } = useAuth();

  if (carregando) return <EstadoCarregando texto="Restaurando sessão…" />;
  if (!usuario) return <Navigate to="/" replace />;

  return (
    <RealtimeProvider>
      <AppShell />
    </RealtimeProvider>
  );
}

/** Redireciona quem já está autenticado para longe da tela de login. */
function AreaPublica() {
  const { usuario, carregando, admin } = useAuth();

  if (carregando) return <EstadoCarregando texto="Restaurando sessão…" />;
  if (usuario) return <Navigate to={admin ? '/dashboard' : '/chamados'} replace />;

  return <Login />;
}

/** O dashboard é a tela inicial do ADMIN; o solicitante entra na própria lista. */
function InicioPorPapel() {
  const { admin } = useAuth();
  return <Navigate to={admin ? '/dashboard' : '/chamados'} replace />;
}

export function App() {
  return (
    <Router>
      <ToastProvider>
        <AuthProvider>
          <Routes>
            <Route path="/" element={<AreaPublica />} />

            <Route element={<AreaAutenticada />}>
              <Route path="/inicio" element={<InicioPorPapel />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/chamados" element={<ListaChamados />} />
              <Route path="/chamados/novo" element={<NovoChamado />} />
              <Route path="/chamados/:id" element={<DetalheChamado />} />
            </Route>

            {/* Rota desconhecida volta para o início em vez de tela em branco. */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </AuthProvider>
      </ToastProvider>
    </Router>
  );
}
