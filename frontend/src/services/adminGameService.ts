import { API_BASE_URL, readError } from "./api";

export interface ReviewGame {
  id: string;
  title: string;
  description: string;
  image: string;
  ownerUsername: string;
  createdAt: string;
}

export interface AdminUserResult {
  username: string;
  email: string;
  role: "ADMIN";
}

export interface AdminSetupResult {
  username: string;
  email: string;
  role: "ADMIN";
}

export async function isFirstAdminSetupAvailable(): Promise<boolean> {
  const response = await fetch(`${API_BASE_URL}/api/admin/setup/status`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) return false;
  const result = await response.json() as { available: boolean };
  return result.available;
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

export async function getDraftGames(): Promise<ReviewGame[]> {
  const response = await fetch(`${API_BASE_URL}/api/admin/games`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}

export async function publishDraft(gameId: string): Promise<void> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!csrfResponse.ok) throw new Error("Could not prepare the publish request.");
  const csrf: CsrfResponse = await csrfResponse.json();
  const response = await fetch(
    `${API_BASE_URL}/api/admin/games/${encodeURIComponent(gameId)}/publish`,
    { method: "POST", credentials: "include", headers: { [csrf.headerName]: csrf.token } },
  );
  if (!response.ok) throw new Error(await readError(response));
}

export async function promoteUserToAdmin(email: string): Promise<AdminUserResult> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!csrfResponse.ok) throw new Error("Could not prepare the admin request.");
  const csrf: CsrfResponse = await csrfResponse.json();
  const response = await fetch(`${API_BASE_URL}/api/admin/users/promote`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ email }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}

export async function setupFirstAdmin(token: string): Promise<AdminSetupResult> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!csrfResponse.ok) throw new Error("Could not prepare the setup request.");
  const csrf: CsrfResponse = await csrfResponse.json();
  const response = await fetch(`${API_BASE_URL}/api/admin/setup`, {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json", [csrf.headerName]: csrf.token },
    body: JSON.stringify({ token }),
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}
