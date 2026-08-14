/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** URL base da API. Vazia em desenvolvimento, pois o Vite faz o proxy de /api. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
