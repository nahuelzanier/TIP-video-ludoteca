import { API_BASE_URL } from "./api";
import type { Game } from "../types/Game";

export async function getGames(): Promise<Game[]> {
    const response = await fetch(`${API_BASE_URL}/api/games`);

    if (!response.ok) {
        throw new Error("No se pudieron cargar los juegos");
    }

    return response.json();
}