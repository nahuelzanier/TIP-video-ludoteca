import { API_BASE_URL } from "./api";
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

    return response.json();
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
