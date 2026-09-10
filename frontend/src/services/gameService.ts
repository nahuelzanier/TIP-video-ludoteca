import type { Game } from "../types/Game";

export async function getGames(): Promise<Game[]> {
    const response = await fetch("http://localhost:8080/api/games");

    if (!response.ok) {
        throw new Error("No se pudieron cargar los juegos");
    }

    return response.json();
}