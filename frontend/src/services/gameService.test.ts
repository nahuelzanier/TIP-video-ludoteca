import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getGameById, getGamesPage, getRandomGame } from "./gameService";

const game = {
  id: "game 1",
  title: "Tiny Quest",
  description: "An adventure",
  image: "/api/games/game%201/cover",
};
let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("gameService", () => {
  it("requests a page with trimmed search text and resolves cover URLs", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      content: [game], page: 2, size: 8, totalElements: 17,
      totalPages: 3, hasNext: true, hasPrevious: true,
    }));

    const result = await getGamesPage(2, 8, "  puzzle  ");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/games?page=2&size=8&q=puzzle",
    );
    expect(result.content[0].image).toBe(
      "http://localhost:8080/api/games/game%201/cover",
    );
    expect(result.hasNext).toBe(true);
  });

  it("encodes game IDs and reports a missing game", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 404 }));

    await expect(getGameById("a/b")).rejects.toThrow("Game not found.");
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/games/a%2Fb");
  });

  it("returns a random game with an API-based image URL", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(game));

    await expect(getRandomGame()).resolves.toMatchObject({
      id: "game 1",
      image: "http://localhost:8080/api/games/game%201/cover",
    });
  });

  it("uses a clear empty-catalog message for a missing random game", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 404 }));

    await expect(getRandomGame()).rejects.toThrow("There are no games available yet.");
  });
});
