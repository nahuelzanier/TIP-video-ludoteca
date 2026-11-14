import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  applyUserSettings,
  getUserSettings,
  saveUserSettings,
  updatePassword,
  updateUsername,
} from "./settingsService";

let fetchMock: ReturnType<typeof vi.fn>;
const savedSettings = { volume: 65, language: "es" as const, theme: "dark" as const };

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("settingsService", () => {
  it("loads settings for the authenticated account without caching", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(savedSettings));

    await expect(getUserSettings()).resolves.toEqual(savedSettings);
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/users/me/settings",
      { credentials: "include", cache: "no-store" },
    );
  });

  it("saves preferences with the session CSRF token", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse(savedSettings));

    await expect(saveUserSettings(savedSettings)).resolves.toEqual(savedSettings);
    expect(fetchMock.mock.calls[1][1]).toEqual({
      method: "PUT",
      credentials: "include",
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
      body: JSON.stringify(savedSettings),
    });
  });

  it("updates the username with CSRF protection", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ username: "new_name" }));

    await expect(updateUsername("new_name")).resolves.toEqual({ username: "new_name" });
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "PATCH",
      body: JSON.stringify({ username: "new_name" }),
    }));
  });

  it("updates the password and accepts the empty 204 response", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(updatePassword("old-password", "new-password")).resolves.toBeUndefined();
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "PATCH",
      body: JSON.stringify({ currentPassword: "old-password", newPassword: "new-password" }),
    }));
  });

  it("applies theme and language to the document", () => {
    const documentMock = { documentElement: { dataset: {} as DOMStringMap, lang: "" } };
    vi.stubGlobal("document", documentMock);

    applyUserSettings(savedSettings);

    expect(documentMock.documentElement.dataset.theme).toBe("dark");
    expect(documentMock.documentElement.lang).toBe("es");
  });
});
