import { API_BASE_URL } from "./api";
import type { Game } from "../types/Game";

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
): Promise<GamePage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

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