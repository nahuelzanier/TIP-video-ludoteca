import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { publishGame, uploadGame } from "./gameUploadService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("gameUploadService", () => {
  it("uploads cover and archive as multipart data with CSRF protection", async () => {
    const cover = new File(["png"], "cover.png", { type: "image/png" });
    const archive = new File(["zip"], "game.zip", { type: "application/zip" });
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: "g-1", title: "Tiny Quest", filesStored: 3 }, 201));

    await expect(uploadGame("Tiny Quest", "An adventure", cover, archive)).resolves.toEqual({
      id: "g-1", title: "Tiny Quest", filesStored: 3,
    });

    const request = fetchMock.mock.calls[1][1] as RequestInit;
    expect(fetchMock.mock.calls[1][0]).toBe("http://localhost:8080/api/games");
    expect(request.method).toBe("POST");
    expect(request.credentials).toBe("include");
    expect(request.headers).toEqual({ "X-XSRF-TOKEN": "csrf" });
    expect(request.body).toBeInstanceOf(FormData);
    const form = request.body as FormData;
    expect(form.get("cover")).toBe(cover);
    expect(form.get("archive")).toBe(archive);
  });

  it("publishes an uploaded game using its encoded ID and CSRF token", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ id: "g/1", title: "Tiny Quest", status: "PUBLISHED" }));

    await expect(publishGame("g/1")).resolves.toMatchObject({ status: "PUBLISHED" });
    expect(fetchMock.mock.calls[1][0]).toBe("http://localhost:8080/api/games/g%2F1/publish");
    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "csrf" },
    }));
  });

  it("does not send an upload when requesting the CSRF token fails", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(uploadGame(
      "Game",
      "",
      new File(["image"], "cover.png"),
      new File(["zip"], "game.zip"),
    )).rejects.toThrow("Could not prepare the upload request.");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("uses the backend's JSON validation message when an upload is rejected", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ detail: "ZIP must contain index.html." }, 400));

    await expect(uploadGame(
      "Game",
      "",
      new File(["image"], "cover.png"),
      new File(["zip"], "game.zip"),
    )).rejects.toThrow("ZIP must contain index.html.");
  });

  it("surfaces plain-text publish errors from the API", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(new Response("Game is not owned by you.", { status: 403 }));

    await expect(publishGame("g-1")).rejects.toThrow("Game is not owned by you.");
  });

  it("does not publish when requesting the CSRF token fails", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(publishGame("g-1")).rejects.toThrow("Could not prepare the publish request.");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
