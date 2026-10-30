import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import type { Game } from "../types/Game";

export interface FavoritePage {
  content: Game[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

function normalizeGame(game: Game): Game {
  return {
    ...game,
    favorite: true,
    image: game.image ? new URL(game.image, API_BASE_URL).toString() : "",
  };
}

export async function getFavoriteGamesPage(
  page = 0,
  size = 9,
): Promise<FavoritePage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const response = await fetch(`${API_BASE_URL}/api/favorites?${params}`, {
    credentials: "include",
    cache: "no-store",
  });

  if (response.status === 401) {
    throw new Error("Iniciá sesión para ver tus favoritos.");
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const result: FavoritePage = await response.json();

  return {
    ...result,
    content: result.content.map(normalizeGame),
  };
}

export async function addFavorite(gameId: string): Promise<boolean> {
  const csrf = await fetchCsrfToken();
  const response = await fetch(
    `${API_BASE_URL}/api/favorites/games/${encodeURIComponent(gameId)}`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (response.status === 401) {
    throw new Error("Iniciá sesión para guardar favoritos.");
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const payload: { favorite?: boolean } = await response.json();
  return payload.favorite ?? true;
}

export async function removeFavorite(gameId: string): Promise<boolean> {
  const csrf = await fetchCsrfToken();
  const response = await fetch(
    `${API_BASE_URL}/api/favorites/games/${encodeURIComponent(gameId)}`,
    {
      method: "DELETE",
      credentials: "include",
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (response.status === 401) {
    throw new Error("Iniciá sesión para quitar favoritos.");
  }

  if (!response.ok && response.status !== 204) {
    throw new Error(await readError(response));
  }

  return false;
}

export async function toggleFavorite(
  gameId: string,
  isFavorite: boolean,
): Promise<boolean> {
  if (isFavorite) {
    await removeFavorite(gameId);
    return false;
  }

  await addFavorite(gameId);
  return true;
}
