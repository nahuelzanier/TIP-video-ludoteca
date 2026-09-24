const baseUrl = import.meta.env.VITE_API_URL;

if (!baseUrl) {
  throw new Error("Falta configurar VITE_API_URL en frontend/.env.local");
}

export const API_BASE_URL = baseUrl.replace(/\/$/, "");