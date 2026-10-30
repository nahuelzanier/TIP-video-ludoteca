import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { fetchCsrfToken, readError, resolveAssetUrl } from "./api";

let fetchMock: ReturnType<typeof vi.fn>;

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("api helpers", () => {
  it("rebases relative assets onto the API host and preserves absent URLs", () => {
    expect(resolveAssetUrl("/api/users/luqui/avatar"))
      .toBe("http://localhost:8080/api/users/luqui/avatar");
    expect(resolveAssetUrl(null)).toBeNull();
    expect(resolveAssetUrl(undefined)).toBeNull();
  });

  it("extracts JSON error messages and falls back to plain response text", async () => {
    await expect(readError(new Response(JSON.stringify({ error: "Denied" }), { status: 403 })))
      .resolves.toBe("Denied");
    await expect(readError(new Response("Bad gateway", { status: 502 })))
      .resolves.toBe("Bad gateway");
    await expect(readError(new Response("", { status: 500 })))
      .resolves.toBe("Error 500");
  });

  it("fetches a CSRF token with cookies and no cache", async () => {
    fetchMock.mockResolvedValueOnce(new Response(JSON.stringify({
      headerName: "X-XSRF-TOKEN", token: "csrf-token",
    })));

    await expect(fetchCsrfToken()).resolves.toEqual({
      headerName: "X-XSRF-TOKEN", token: "csrf-token",
    });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/auth/csrf",
      { credentials: "include", cache: "no-store" },
    );
  });

  it("rejects when a CSRF token cannot be fetched", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 403 }));

    await expect(fetchCsrfToken()).rejects.toThrow("No se pudo preparar la solicitud.");
  });
});
