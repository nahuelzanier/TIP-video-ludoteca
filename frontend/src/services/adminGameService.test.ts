import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getDraftGames,
  isFirstAdminSetupAvailable,
  promoteUserToAdmin,
  publishDraft,
  setupFirstAdmin,
} from "./adminGameService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("adminGameService", () => {
  it("checks whether the first admin setup is available", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ available: true }));

    await expect(isFirstAdminSetupAvailable()).resolves.toBe(true);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/admin/setup/status",
      { credentials: "include", cache: "no-store" },
    );
  });

  it("loads pending draft games without caching", async () => {
    const drafts = [{
      id: "draft-1",
      title: "Unreleased game",
      description: "Ready for review",
      image: "/games/draft-1/cover.png",
      ownerUsername: "creator",
      createdAt: "2026-11-01T12:00:00Z",
    }];
    fetchMock.mockResolvedValueOnce(jsonResponse(drafts));

    await expect(getDraftGames()).resolves.toEqual(drafts);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/admin/games",
      { credentials: "include", cache: "no-store" },
    );
  });

  it("publishes an encoded draft ID with CSRF protection", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(publishDraft("draft/one")).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[1]).toEqual([
      "http://localhost:8080/api/admin/games/draft%2Fone/publish",
      {
        method: "POST",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": "csrf" },
      },
    ]);
  });

  it("promotes a registered user to admin using CSRF protection", async () => {
    const promoted = { username: "reviewer", email: "reviewer@example.com", role: "ADMIN" as const };
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse(promoted));

    await expect(promoteUserToAdmin("reviewer@example.com")).resolves.toEqual(promoted);
    expect(fetchMock.mock.calls[1]).toEqual([
      "http://localhost:8080/api/admin/users/promote",
      {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
        body: JSON.stringify({ email: "reviewer@example.com" }),
      },
    ]);
  });

  it("sets up the first admin account with the provided setup token", async () => {
    const admin = { username: "owner", email: "owner@example.com", role: "ADMIN" as const };
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse(admin));

    await expect(setupFirstAdmin("one-time-key")).resolves.toEqual(admin);
    expect(fetchMock.mock.calls[1]).toEqual([
      "http://localhost:8080/api/admin/setup",
      {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
        body: JSON.stringify({ token: "one-time-key" }),
      },
    ]);
  });

  it("reports when the CSRF token cannot be loaded before publishing", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 503 }));

    await expect(publishDraft("draft-1")).rejects.toThrow(
      "Could not prepare the publish request.",
    );
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
