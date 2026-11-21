import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    port: 3000,
    host: true,
    proxy: {
      '/api': {
        // Đổi cổng backend khi cần: VITE_PROXY_TARGET=http://localhost:18080 npm run dev
        target: process.env.VITE_PROXY_TARGET || 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Request đi qua proxy là cùng origin với trang. Bỏ header Origin để điện thoại vào qua IP LAN
        // (http://192.168.x.x:3000) khi demo máy chấm công không bị CORS của backend chặn.
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
});
