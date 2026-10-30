import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { createComment, getGameComments, updateComment } from "./commentService";

let fetchMock: ReturnType<typeof vi.fn>;
const comment = {
  id: 5, content: "Nice game", rating: 5, createdAt: "2026-10-01T00:00:00Z",
  edited: false, username: "luqui", profileImageUrl: null, isGameAuthor: false, replies: [],
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("commentService", () => {
  it("loads comments for an encoded game ID", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([comment]));

    await expect(getGameComments("a/b")).resolves.toEqual([comment]);
    expect(fetchMock).toHaveBeenCalledWith("http://localhost:8080/api/games/a%2Fb/comments");
  });

  it("creates a comment with the CSRF token", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse(comment, 201));

    await expect(createComment("g-1", { content: "Nice game", rating: 5 })).resolves.toEqual(comment);
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
      body: JSON.stringify({ content: "Nice game", rating: 5 }),
    }));
  });

  it("explains authorization and ownership failures when editing a comment", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(updateComment(5, "Edited")).rejects.toThrow(
      "No podés editar un comentario de otro usuario.",
    );
  });

  it("explains expired sessions when posting a comment", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));

    await expect(createComment("g-1", { content: "Hello" })).rejects.toThrow(
      "Tu sesión expiró. Volvé a iniciar sesión.",
    );
  });
});
