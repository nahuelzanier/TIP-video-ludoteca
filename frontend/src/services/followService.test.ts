import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  followUser,
  getFollowCounts,
  getFollowStatus,
  unfollowUser,
} from "./followService";

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

describe("followService", () => {
  it("reads the authenticated follow status", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ username: "Ana", following: true }));

    await expect(getFollowStatus("Ana")).resolves.toEqual({
      username: "Ana",
      following: true,
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/follows/Ana/status",
      { credentials: "include", cache: "no-store" },
    );
  });

  it("treats an anonymous status as not following", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(getFollowStatus("Ana")).resolves.toEqual({
      username: "Ana",
      following: false,
    });
  });

  it("reads public counters", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      username: "Ana",
      followersCount: 12,
      followingCount: 4,
    }));

    await expect(getFollowCounts("Ana")).resolves.toMatchObject({
      followersCount: 12,
      followingCount: 4,
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/follows/Ana/counts",
      { cache: "no-store" },
    );
  });

  it("follows with CSRF and returns the server status", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse({ username: "Ana", following: true }));

    await expect(followUser("Ana")).resolves.toBe(true);
    expect(fetchMock.mock.calls[1]).toEqual([
      "http://localhost:8080/api/follows/Ana",
      expect.objectContaining({
        method: "PUT",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": "csrf" },
      }),
    ]);
  });

  it("unfollows with CSRF and accepts 204", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(unfollowUser("Ana")).resolves.toBe(false);
    expect(fetchMock.mock.calls[1][1].method).toBe("DELETE");
  });

  it("surfaces authentication and server errors", async () => {
    fetchMock.mockResolvedValueOnce(csrfResponse());
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(followUser("Ana")).rejects.toThrow("Log in to follow creators.");

    fetchMock.mockResolvedValueOnce(csrfResponse());
    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "Rejected" }, 400));
    await expect(followUser("Ana")).rejects.toThrow("Rejected");
  });
});
