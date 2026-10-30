import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createForumDiscussion,
  createForumReply,
  getForumDiscussion,
  getForumDiscussions,
  getForumReplies,
  rateForumDiscussion,
} from "./forumService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("forumService", () => {
  it("resolves discussion cover and avatar URLs from the API origin", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      content: [{
        id: 8, title: "Discussion", description: "Review", createdAt: "2026-10-01T00:00:00Z",
        gameId: "g-1", gameTitle: "Game", authorUsername: "luqui",
        authorAvatarUrl: "/api/users/luqui/avatar", coverImageUrl: "/api/forum/discussions/8/cover",
        tags: [], averageRating: null, ratingCount: 0,
      }],
      page: 0, size: 12, totalElements: 1, totalPages: 1, hasNext: false, hasPrevious: false,
    }));

    const result = await getForumDiscussions();

    expect(result.content[0].coverImageUrl)
      .toBe("http://localhost:8080/api/forum/discussions/8/cover");
    expect(result.content[0].authorAvatarUrl)
      .toBe("http://localhost:8080/api/users/luqui/avatar");
  });

  it("creates a discussion without a cover while including CSRF and repeated tags", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: 8, title: "Discussion" }, 201));

    await expect(createForumDiscussion({
      gameId: "game-1", title: "Discussion", description: "Description", cover: null, tagIds: [2, 5],
    })).resolves.toEqual({ id: 8, title: "Discussion" });

    const request = fetchMock.mock.calls[1][1] as RequestInit;
    expect(request.method).toBe("POST");
    expect(request.headers).toEqual({ "X-XSRF-TOKEN": "csrf" });
    expect(request.credentials).toBe("include");
    expect(request.body).toBeInstanceOf(FormData);
    const form = request.body as FormData;
    expect(form.get("cover")).toBeNull();
    expect(form.getAll("tagIds")).toEqual(["2", "5"]);
  });

  it("loads a discussion detail with the current user's rating and resolved image URLs", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      id: 8, title: "Discussion", description: "Review", createdAt: "2026-10-01T00:00:00Z",
      gameId: "g-1", gameTitle: "Game", authorUsername: "luqui",
      authorAvatarUrl: "/api/users/luqui/avatar", coverImageUrl: "/api/forum/discussions/8/cover",
      tags: [], averageRating: 4, ratingCount: 2, myRating: 5,
    }));

    const result = await getForumDiscussion(8);

    expect(result.myRating).toBe(5);
    expect(result.coverImageUrl).toBe("http://localhost:8080/api/forum/discussions/8/cover");
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/forum/discussions/8",
      { credentials: "include" },
    );
  });

  it("loads replies and resolves each reply author's avatar URL", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse([{
      id: 10, content: "I agree", createdAt: "2026-10-01T00:00:00Z",
      authorUsername: "luqui", authorAvatarUrl: "/api/users/luqui/avatar", parentReplyId: null,
    }]));

    const replies = await getForumReplies(8);

    expect(replies[0].authorAvatarUrl).toBe("http://localhost:8080/api/users/luqui/avatar");
  });

  it("creates a nested reply and rates a discussion with CSRF protection", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: 10, content: "I agree" }, 201))
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({
        discussionId: 8, myRating: 4, averageRating: 4, ratingCount: 3,
      }));

    await expect(createForumReply(8, "I agree", 6)).resolves.toMatchObject({ content: "I agree" });
    await expect(rateForumDiscussion(8, 4)).resolves.toMatchObject({ myRating: 4 });

    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "POST",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
      body: JSON.stringify({ content: "I agree", parentReplyId: 6 }),
    }));
    expect(fetchMock.mock.calls[3][1]).toEqual(expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({ rating: 4 }),
    }));
  });

  it("surfaces the API message when creating a discussion fails", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ error: "Choose a published game." }, 400));

    await expect(createForumDiscussion({
      gameId: "draft", title: "Discussion", description: "Description", cover: null, tagIds: [2],
    })).rejects.toThrow("Choose a published game.");
  });
});
