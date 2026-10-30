import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { assignGameTag, getAvailableTags, getGameTags, getMyGameTagIds } from "./tagService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("tagService", () => {
  it("requests the signed-in user's assigned tag IDs with session cookies", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([3, 4]));

    await expect(getMyGameTagIds("game/1")).resolves.toEqual([3, 4]);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/games/game%2F1/tags/mine",
      expect.objectContaining({ credentials: "include", cache: "no-store" }),
    );
  });

  it("loads the public tag catalog and a game's ordered tag summaries", async () => {
    const catalog = [{ id: 3, name: "Puzzle", slug: "puzzle" }];
    const assigned = [{
      tagId: 3, name: "Puzzle", slug: "puzzle", assignmentCount: 4,
      firstAssignedAt: "2026-10-01T00:00:00Z",
    }];
    fetchMock
      .mockResolvedValueOnce(jsonResponse(catalog))
      .mockResolvedValueOnce(jsonResponse(assigned));

    await expect(getAvailableTags()).resolves.toEqual(catalog);
    await expect(getGameTags("game-1")).resolves.toEqual(assigned);
    expect(fetchMock.mock.calls.map(([url]) => url)).toEqual([
      "http://localhost:8080/api/tags",
      "http://localhost:8080/api/games/game-1/tags",
    ]);
  });

  it("assigns a tag with CSRF and reports conflict messages returned by the API", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ error: "Already assigned." }, 409));

    await expect(assignGameTag("game-1", 6)).rejects.toThrow("Already assigned.");
    const request = fetchMock.mock.calls[1][1] as RequestInit;
    expect(request.method).toBe("POST");
    expect(request.credentials).toBe("include");
    expect(request.headers).toEqual({
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": "csrf",
    });
    expect(request.body).toBe(JSON.stringify({ tagId: 6 }));
  });

  it("uses the response text when an API error is not JSON", async () => {
    fetchMock.mockResolvedValueOnce(new Response("CSRF rejected", { status: 403 }));

    await expect(getMyGameTagIds("game-1")).rejects.toThrow("CSRF rejected");
  });

  it("does not send a tag assignment when obtaining the CSRF token fails", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(assignGameTag("game-1", 6)).rejects.toThrow(
      "Could not prepare the tag request.",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
