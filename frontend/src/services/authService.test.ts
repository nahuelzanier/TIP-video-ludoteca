import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { getCurrentUser, login, logout, register } from "./authService";

const csrf = { headerName: "X-XSRF-TOKEN", token: "csrf-value" };
const currentUser = { id: 4, username: "luqui", email: "luqui@example.test" };
let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("authService", () => {
  it("logs in after fetching a CSRF token and sends the token with credentials", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(csrf))
      .mockResolvedValueOnce(jsonResponse(currentUser));

    await expect(login(currentUser.email, "secret")).resolves.toEqual(currentUser);

    expect(fetchMock).toHaveBeenNthCalledWith(
      1,
      "http://localhost:8080/api/auth/csrf",
      expect.objectContaining({ credentials: "include", cache: "no-store" }),
    );
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "http://localhost:8080/api/auth/login",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": "csrf-value" },
        body: JSON.stringify({ email: currentUser.email, password: "secret" }),
      }),
    );
  });

  it("stops registration when the CSRF token request fails", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "forbidden" }, 403));

    await expect(register("luqui", currentUser.email, "secret"))
      .rejects.toThrow("No se pudo iniciar la solicitud de autenticación.");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("returns null when there is no authenticated session", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));

    await expect(getCurrentUser()).resolves.toBeNull();
  });

  it("rejects when loading the current user fails for a non-authentication reason", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 500 }));

    await expect(getCurrentUser()).rejects.toThrow("Could not load the current user.");
  });

  it("logs out with the CSRF token and session credentials", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse(csrf))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(logout()).resolves.toBeUndefined();
    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      "http://localhost:8080/api/auth/logout",
      expect.objectContaining({
        method: "POST",
        credentials: "include",
        headers: { "X-XSRF-TOKEN": "csrf-value" },
      }),
    );
  });
});
