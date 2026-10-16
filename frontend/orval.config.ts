import { defineConfig } from "orval";

/**
 * Generates the typed client from the committed backend contract into `src/shared/api/generated/`:
 * TanStack Query hooks (routed through `httpClient`), MSW handlers for tests, and zod schemas the
 * submit form reuses. `npm run api:check` regenerates and fails on any diff.
 */
export default defineConfig({
  endpoints: {
    input: { target: "../openapi/openapi.json" },
    output: {
      target: "src/shared/api/generated/endpoints.ts",
      schemas: "src/shared/api/generated/model",
      client: "react-query",
      mode: "single",
      mock: { generators: [{ type: "msw" }] },
      clean: true,
      prettier: false,
      override: {
        mutator: { path: "src/shared/api/http-client.ts", name: "httpClient" },
        fetch: { includeHttpResponseReturnType: false },
      },
    },
  },
  zod: {
    input: { target: "../openapi/openapi.json" },
    output: {
      target: "src/shared/api/generated/zod.ts",
      client: "zod",
      mode: "single",
      fileExtension: ".ts",
      prettier: false,
    },
  },
});
