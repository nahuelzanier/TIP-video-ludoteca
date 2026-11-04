import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  addFavorite,
  getFavoriteGamesPage,
  removeFavorite,
  toggleFavorite,
} from "./favoriteService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

function csrfResponse(): Response {
  return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("favoriteService", () => {
  it("loads a page and marks returned games as favorites", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      content: [
        { id: "g-1", title: "Game", description: "", image: "/cover.png" },
        { id: "g-2", title: "No cover", description: "", image: "" },
      ],
      page: 1,
      size: 9,
      totalElements: 10,
      totalPages: 2,
      hasNext: false,
      hasPrevious: true,
    }));

    const result = await getFavoriteGamesPage(1, 9);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/favorites?page=1&size=9",
      { credentials: "include", cache: "no-store" },
    );
    expect(result.content[0]).toMatchObject({
      id: "g-1",
      favorite: true,
      image: "http://localhost:8080/cover.png",
    });
    expect(result.content[1]).toMatchObject({ favorite: true, image: "" });
    expect(result.hasPrevious).toBe(true);
  });

  it("reports when the favorites page requires login or fails", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(getFavoriteGamesPage()).rejects.toThrow("Log in to view your favorites.");

    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "Unavailable" }, 500));
    await expect(getFavoriteGamesPage()).rejects.toThrow("Unavailable");
  });

  it("adds a favorite with CSRF protection and uses true when the response omits it", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({}));

    await expect(addFavorite("game/1")).resolves.toBe(true);
    expect(fetchMock.mock.calls[1][0]).toBe(
      "http://localhost:8080/api/favorites/games/game%2F1",
    );
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
    }));
  });

  it("returns the server favorite value and handles add errors", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ favorite: false }));
    await expect(addFavorite("g-1")).resolves.toBe(false);

    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(addFavorite("g-1")).rejects.toThrow("Log in to add favorites.");

    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ error: "Rejected" }, 400));
    await expect(addFavorite("g-1")).rejects.toThrow("Rejected");
  });

  it("removes a favorite with CSRF protection and returns false for 204", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(removeFavorite("g-1")).resolves.toBe(false);
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "DELETE",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "csrf" },
    }));
  });

  it("handles remove errors while allowing a successful non-204 response", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 200 }));
    await expect(removeFavorite("g-1")).resolves.toBe(false);

    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(removeFavorite("g-1")).rejects.toThrow("Log in to remove favorites.");

    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ error: "Rejected" }, 500));
    await expect(removeFavorite("g-1")).rejects.toThrow("Rejected");
  });

  it("toggles in both directions", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ favorite: true }));
    await expect(toggleFavorite("g-1", false)).resolves.toBe(true);

    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    await expect(toggleFavorite("g-1", true)).resolves.toBe(false);
  });
});
