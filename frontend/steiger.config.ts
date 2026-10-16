import fsd from "@feature-sliced/steiger-plugin";
import { defineConfig } from "steiger";

export default defineConfig([
  ...fsd.configs.recommended,
  {
    rules: {
      // A compact demo uses most slices exactly once by design, so this heuristic only yields noise.
      "fsd/insignificant-slice": "off",
      // The `application-*` prefix carries meaning: all views of one aggregate.
      "fsd/repetitive-naming": "off",
      // The design system is one file per component, not grouped by kind.
      "fsd/shared-lib-grouping": "off",
    },
  },
  {
    ignores: ["./src/shared/api/generated/**", "./src/app/route-tree.gen.ts"],
  },
]);
