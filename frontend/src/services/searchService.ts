import { API_BASE_URL, resolveAssetUrl } from "./api";
import type { SearchResponse } from "../types/Search";

export async function searchEverything(
    q: string,
    gamesPage = 0,
    usersPage = 0,
    size = 9,
): Promise<SearchResponse> {
    const params = new URLSearchParams({
        q: q.trim(),
        gamesPage: String(gamesPage),
        usersPage: String(usersPage),
        size: String(size),
    });

    const response = await fetch(`${API_BASE_URL}/api/search?${params.toString()}`);

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const result: SearchResponse = await response.json();

    return {
        ...result,
        games: {
            ...result.games,
            items: result.games.items.map((game) => ({
                ...game,
                image: game.image
                    ? new URL(game.image, API_BASE_URL).toString()
                    : "",
            })),
        },
        users: {
            ...result.users,
            items: result.users.items.map((user) => ({
                ...user,
                avatarUrl: resolveAssetUrl(user.avatarUrl),
            })),
        },
    };
}

async function readError(response: Response): Promise<string> {
    const text = await response.text();

    try {
        const error = JSON.parse(text) as { error?: string };
        return error.error || text || `Error ${response.status}`;
    } catch {
        return text || `Error ${response.status}`;
    }
}
