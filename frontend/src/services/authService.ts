import { API_BASE_URL } from "./api";

export interface AuthUser {
  id: number;
  username: string;
  email: string;
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

async function postAuth(
  endpoint: string,
  data: Record<string, string>,
): Promise<AuthUser> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!csrfResponse.ok) {
    throw new Error("No se pudo iniciar la solicitud de autenticación.");
  }

  const csrf: CsrfResponse = await csrfResponse.json();

  const response = await fetch(`${API_BASE_URL}${endpoint}`, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(data),
  });

  if (!response.ok) {
    const text = await response.text();

    try {
      const error = JSON.parse(text);
      throw new Error(error.error ?? error.message ?? "La solicitud falló.");
    } catch (error) {
      if (error instanceof Error && error.message !== "La solicitud falló.") {
        throw error;
      }
      throw new Error(text || `Error ${response.status}`);
    }
  }

  return response.json();
}

export function register(
  username: string,
  email: string,
  password: string,
): Promise<AuthUser> {
  return postAuth("/api/auth/register", { username, email, password });
}

export function login(email: string, password: string): Promise<AuthUser> {
  return postAuth("/api/auth/login", { email, password });
}