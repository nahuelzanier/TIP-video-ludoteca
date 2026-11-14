import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import { setAppLanguage } from "./i18n";

export interface UserSettings {
  volume: number;
  language: "en" | "es";
  theme: "system" | "light" | "dark";
}

let useSystemTheme = false;
let systemThemeQuery: MediaQueryList | null = null;

function applyTheme() {
  if (!systemThemeQuery && typeof window !== "undefined") {
    systemThemeQuery = window.matchMedia("(prefers-color-scheme: dark)");
    systemThemeQuery.addEventListener("change", () => {
      if (useSystemTheme) {
        document.documentElement.dataset.theme = systemThemeQuery?.matches ? "dark" : "light";
      }
    });
  }

  document.documentElement.dataset.theme = useSystemTheme
    ? systemThemeQuery?.matches ? "dark" : "light"
    : document.documentElement.dataset.theme;
}

async function sendJson<T>(path: string, method: string, body: unknown): Promise<T> {
  const csrf = await fetchCsrfToken();
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) throw new Error(await readError(response));
  if (response.status === 204) return undefined as T;
  return response.json();
}

export async function getUserSettings(): Promise<UserSettings> {
  const response = await fetch(`${API_BASE_URL}/api/users/me/settings`, {
    credentials: "include",
    cache: "no-store",
  });
  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}

export function saveUserSettings(settings: UserSettings): Promise<UserSettings> {
  return sendJson("/api/users/me/settings", "PUT", settings);
}

export async function updateUsername(username: string): Promise<{ username: string }> {
  return sendJson("/api/users/me/username", "PATCH", { username });
}

export function updatePassword(currentPassword: string, newPassword: string): Promise<void> {
  return sendJson("/api/users/me/password", "PATCH", { currentPassword, newPassword });
}

export function applyUserSettings(settings: UserSettings) {
  useSystemTheme = settings.theme === "system";
  if (settings.theme !== "system") {
    document.documentElement.dataset.theme = settings.theme;
  }
  applyTheme();
  document.documentElement.lang = settings.language;
  setAppLanguage(settings.language);
}
