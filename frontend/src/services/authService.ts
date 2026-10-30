import { API_BASE_URL, readError } from "./api";

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
    throw new Error(await readError(response));
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

export async function getCurrentUser(): Promise<AuthUser | null> {
  const response = await fetch(`${API_BASE_URL}/api/auth/me`, {
    credentials: "include",
    cache: "no-store",
  });

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error("Could not load the current user.");
  }

  return response.json();
}

export async function logout(): Promise<void> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!csrfResponse.ok) {
    throw new Error("Could not prepare the logout request.");
  }

  const csrf: CsrfResponse = await csrfResponse.json();

  const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
    method: "POST",
    credentials: "include",
    headers: {
      [csrf.headerName]: csrf.token,
    },
  });

  if (!response.ok) {
    throw new Error("Could not log out. Please try again.");
  }
}