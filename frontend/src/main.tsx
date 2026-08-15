import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { App } from './App';
import { TemaProvider } from './styles/TemaContext';
import './styles/global.css';

const raiz = document.getElementById('root');

if (!raiz) {
  throw new Error('Elemento #root não encontrado no index.html.');
}

createRoot(raiz).render(
  <StrictMode>
    {/* Envolve tudo: o tema vale inclusive na tela de entrada, antes de haver
        usuario autenticado. */}
    <TemaProvider>
      <App />
    </TemaProvider>
  </StrictMode>,
);
