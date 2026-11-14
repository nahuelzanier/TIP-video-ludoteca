import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import type { GameCustomization } from "../types/Game";

export async function saveGameCustomization(
  gameId: string,
  customization: GameCustomization,
): Promise<GameCustomization> {
  const csrf = await fetchCsrfToken();
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/customization`,
    {
      method: "PUT",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify(customization),
    },
  );

  if (!response.ok) throw new Error(await readError(response));
  return response.json();
}
