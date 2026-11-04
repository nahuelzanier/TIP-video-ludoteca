import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { deleteAvatar, getUserForums, getUserGamesPage, getUserProfile, updateDescription, uploadAvatar } from "./userService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("userService", () => {
  it("returns null for an unknown public profile and encodes usernames", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 404 }));

    await expect(getUserProfile("user/name")).resolves.toBeNull();
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/users/user%2Fname/profile");
  });

  it("loads the discussions created by a user and encodes the username", async () => {
    const forums = [{ id: 8, title: "A discussion", createdAt: "2026-10-30T10:00:00Z" }];
    fetchMock.mockResolvedValueOnce(jsonResponse(forums));

    await expect(getUserForums("user/name")).resolves.toEqual(forums);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/user%2Fname/forums",
    );
  });

  it("reports an API error when a user's discussions cannot be loaded", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "Could not load discussions." }, 500));

    await expect(getUserForums("luqui")).rejects.toThrow("Could not load discussions.");
  });

  it("updates a description with a CSRF token and resolves an avatar URL", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: 4, username: "luqui", description: "Designer", avatarUrl: "/api/users/luqui/avatar" }));

    await expect(updateDescription("luqui", "Designer")).resolves.toMatchObject({
      avatarUrl: "http://localhost:8080/api/users/luqui/avatar",
    });
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "PATCH",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
      body: JSON.stringify({ description: "Designer" }),
    }));
  });

  it("uploads an avatar as multipart data without overriding its content type", async () => {
    const avatar = new File(["avatar"], "avatar.png", { type: "image/png" });
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: 4, username: "luqui", description: null, avatarUrl: null }));

    await uploadAvatar("luqui", avatar);
    const request = fetchMock.mock.calls[1][1] as RequestInit;
    expect(request.method).toBe("PUT");
    expect(request.headers).toEqual({ "X-XSRF-TOKEN": "csrf" });
    expect((request.body as FormData).get("avatar")).toBe(avatar);
  });

  it("deletes the avatar with CSRF protection", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: 4, username: "luqui", description: null, avatarUrl: null }));

    await deleteAvatar("luqui");
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "DELETE",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "csrf" },
    }));
  });

  it("loads a user's games and resolves each game image URL", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      content: [{ id: "g-1", title: "Game", image: "/api/games/g-1/cover", description: "", favorito: true }],
      page: 1, size: 12, totalElements: 13, totalPages: 2, hasNext: false, hasPrevious: true,
    }));

    const page = await getUserGamesPage("luqui", 1);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/luqui/games?page=1&size=12",
      { credentials: "include", cache: "no-store" },
    );
    expect(page.content[0].image).toBe("http://localhost:8080/api/games/g-1/cover");
    expect(page.content[0].favorite).toBe(true);
    expect(page.hasPrevious).toBe(true);
  });
});
