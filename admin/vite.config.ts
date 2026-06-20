import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001,
    host: true,
    allowedHosts: ['admin.bhramankaro.com', '.bhramankaro.com', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  // Static production serving (no hot-reload drops -> no transient 502s through the tunnel).
  // Mirrors the dev server: same port, host allowlist, and /api proxy.
  preview: {
    port: 3001,
    strictPort: true,
    host: true,
    allowedHosts: ['admin.bhramankaro.com', '.bhramankaro.com', 'localhost'],
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
