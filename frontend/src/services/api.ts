const gameContentUrl = import.meta.env.VITE_GAME_CONTENT_URL ?? "http://127.0.0.1:8080";
export const GAME_CONTENT_BASE_URL = gameContentUrl.replace(/\/$/, "");

const baseUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080";
export const API_BASE_URL = baseUrl.replace(/\/$/, "");

/**
 * The backend returns asset paths relative to itself ("/api/..."). Resolving them
 * against the frontend origin would point the request at Vite, so they have to be
 * rebased onto the API origin before they reach an <img src>.
 */
export function resolveAssetUrl(path: string | null | undefined): string | null {
  if (!path) {
    return null;
  }

  return new URL(path, API_BASE_URL).toString();
}

export interface CsrfToken {
  headerName: string;
  token: string;
}

export async function readError(response: Response): Promise<string> {
  const text = await response.text();

  try {
    const error = JSON.parse(text) as {
      detail?: string;
      message?: string;
      error?: string;
    };
    return error.detail || error.message || error.error || text || `Error ${response.status}`;
  } catch {
    return text || `Error ${response.status}`;
  }
}

export async function fetchCsrfToken(): Promise<CsrfToken> {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("No se pudo preparar la solicitud.");
  }

  return response.json();
}
