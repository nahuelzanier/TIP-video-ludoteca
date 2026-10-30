import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { deleteAvatar, getUserGamesPage, getUserProfile, updateDescription, uploadAvatar } from "./userService";

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
      content: [{ id: "g-1", title: "Game", image: "/api/games/g-1/cover", description: "" }],
      page: 1, size: 12, totalElements: 13, totalPages: 2, hasNext: false, hasPrevious: true,
    }));

    const page = await getUserGamesPage("luqui", 1);

    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/users/luqui/games?page=1&size=12");
    expect(page.content[0].image).toBe("http://localhost:8080/api/games/g-1/cover");
    expect(page.hasPrevious).toBe(true);
  });
});
