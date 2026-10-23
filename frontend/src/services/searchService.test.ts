import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { searchEverything } from "./searchService";

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("searchService", () => {
  it("trims and encodes the query and resolves game and avatar asset URLs", async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({
      query: "puzzle games",
      games: {
        items: [{ id: "g-1", title: "Puzzle", image: "/api/games/g-1/cover", description: "" }],
        page: 0, size: 9, totalItems: 1, totalPages: 1,
      },
      users: {
        items: [{ id: 3, username: "luqui", avatarUrl: "/api/users/luqui/avatar" }],
        page: 0, size: 9, totalItems: 1, totalPages: 1,
      },
    })));

    const result = await searchEverything("  puzzle games ", 1, 2, 6);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/search?q=puzzle+games&gamesPage=1&usersPage=2&size=6",
    );
    expect(result.games.items[0].image).toBe("http://localhost:8080/api/games/g-1/cover");
    expect(result.users.items[0].avatarUrl).toBe("http://localhost:8080/api/users/luqui/avatar");
  });

  it("sends combined genre and rating filters for games", async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({
      query: "puzzle",
      games: { items: [], page: 0, size: 9, totalItems: 0, totalPages: 0 },
      users: { items: [], page: 0, size: 9, totalItems: 0, totalPages: 0 },
    })));

    await searchEverything("puzzle", 0, 0, 9, { tagIds: [2, 7], minRating: 4 });

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/search?q=puzzle&gamesPage=0&usersPage=0&size=9&tagIds=2&tagIds=7&minRating=4",
    );
  });

  it("uses empty strings for absent game images and avatars", async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({
      query: "empty",
      games: { items: [{ id: "g", title: "Game", image: "", description: "" }], page: 0, size: 9, totalItems: 1, totalPages: 1 },
      users: { items: [{ id: 3, username: "luqui", avatarUrl: null }], page: 0, size: 9, totalItems: 1, totalPages: 1 },
    })));

    const result = await searchEverything("empty");

    expect(result.games.items[0].image).toBe("");
    expect(result.users.items[0].avatarUrl).toBeNull();
  });

  it("surfaces a readable API error", async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({ error: "Query too short" }), { status: 400 }));

    await expect(searchEverything("a")).rejects.toThrow("Query too short");
  });
});
