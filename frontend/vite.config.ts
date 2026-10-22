import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
export default defineConfig(({ mode }) => {
  const target = loadEnv(mode, process.cwd(), '').API_PROXY_TARGET || 'http://127.0.0.1:8080';
  const proxy = { '/api': { target, changeOrigin: true } };
  return {
    plugins: [react()],
    resolve: { preserveSymlinks: true },
    server: { host: '127.0.0.1', port: 5173, strictPort: true, proxy },
    preview: { host: '127.0.0.1', port: 5173, strictPort: true, proxy },
  };
});
