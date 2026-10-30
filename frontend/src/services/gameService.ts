import { API_BASE_URL } from "./api";
import type { Game, GameRecommendation } from "../types/Game";
import type { GameFilters } from "../types/GameFilters";

export interface GamePage {
  content: Game[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export async function getGamesPage(
  page = 0,
  size = 12,
  query = "",
  filters: GameFilters = { tagIds: [], minRating: null },
): Promise<GamePage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (query.trim()) {
    params.set("q", query.trim());
  }
  filters.tagIds.forEach((tagId) => params.append("tagIds", String(tagId)));
  if (filters.minRating !== null) {
    params.set("minRating", String(filters.minRating));
  }

  const response = await fetch(`${API_BASE_URL}/api/games?${params}`);

  if (!response.ok) {
    throw new Error("No se pudieron cargar los juegos");
  }

  const result: GamePage = await response.json();

  return {
    ...result,
    content: result.content.map((game) => ({
      ...game,
      image: new URL(game.image, API_BASE_URL).toString(),
    })),
  };
}

// Mantiene funcionando temporalmente la página Game, que todavía usa getGames().
export async function getGames(): Promise<Game[]> {
  const result = await getGamesPage(0, 24);
  return result.content;
}

export async function getGameById(gameId: string): Promise<Game> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}`,
  );

  if (!response.ok) {
    throw new Error(
      response.status === 404 ? "Game not found." : "Could not load the game.",
    );
  }

  const game: Game = await response.json();

  return {
    ...game,
    image: new URL(game.image, API_BASE_URL).toString(),
  };
}

export async function getRandomGame(): Promise<Game> {
  const response = await fetch(`${API_BASE_URL}/api/games/random`);

  if (response.status === 404) {
    throw new Error("There are no games available yet.");
  }

  if (!response.ok) {
    throw new Error("Could not load a random game.");
  }

  const game: Game = await response.json();

  return {
    ...game,
    image: new URL(game.image, API_BASE_URL).toString(),
  };
}

export async function getGameRecommendations(
  gameId: string,
): Promise<GameRecommendation[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/recommendations`,
  );

  if (!response.ok) {
    throw new Error(
      response.status === 404
        ? "Game not found."
        : "Could not load recommendations.",
    );
  }

  const recommendations: GameRecommendation[] = await response.json();

  return recommendations.map((game) => ({
    ...game,
    image: game.image ? new URL(game.image, API_BASE_URL).toString() : "",
  }));
}
