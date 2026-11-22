import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// В dev-режиме /api проксируется на шлюз (GATEWAY_URL), чтобы не настраивать CORS.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: { '/api': { target: process.env.GATEWAY_URL ?? 'http://localhost:8080', rewrite: p => p.replace(/^\/api/, '') } } },
})
