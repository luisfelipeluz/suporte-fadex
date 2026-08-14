import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // O frontend fala com `/api` na própria origem e o Vite encaminha para o
    // backend. Assim o código não precisa de URL absoluta em desenvolvimento e
    // não há CORS no fluxo do dia a dia.
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET ?? 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  preview: {
    port: 4173,
  },
});
