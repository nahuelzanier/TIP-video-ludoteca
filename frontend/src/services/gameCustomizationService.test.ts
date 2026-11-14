import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { saveGameCustomization } from "./gameCustomizationService";
import type { GameCustomization } from "../types/Game";

const appearance: GameCustomization = {
  backgroundColor: "#102030",
  accentColor: "#d080ff",
  textColor: "#eeeeee",
  fontFamily: "GEORGIA",
  backgroundStyle: "GRADIENT",
};

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("gameCustomizationService", () => {
  it("saves the appearance with CSRF protection and an encoded game ID", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse(appearance));

    await expect(saveGameCustomization("game/a", appearance)).resolves.toEqual(appearance);

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "http://localhost:8080/api/games/game%2Fa/customization",
      {
        method: "PUT",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf" },
        body: JSON.stringify(appearance),
      },
    );
  });

  it("surfaces the backend error when saving is rejected", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" }))
      .mockResolvedValueOnce(jsonResponse({ error: "No tenés permiso." }, 403));

    await expect(saveGameCustomization("game-1", appearance))
      .rejects.toThrow("No tenés permiso.");
  });
});
