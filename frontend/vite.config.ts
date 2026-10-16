import { defineConfig } from "vite";
import { tanstackRouter } from "@tanstack/router-plugin/vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

const BACKEND = "http://localhost:8081";

export default defineConfig({
  plugins: [
    tanstackRouter({
      target: "react",
      routesDirectory: "src/app/routes",
      generatedRouteTree: "src/app/route-tree.gen.ts",
      autoCodeSplitting: true,
    }),
    react(),
    tailwindcss(),
  ],
  resolve: { alias: { "@": "/src" } },
  server: {
    port: 5173,
    proxy: {
      "/api": BACKEND,
      "/v3/api-docs": BACKEND,
      "/engine-rest": BACKEND,
      "/camunda": BACKEND,
      "/webapp": BACKEND,
    },
  },
});
