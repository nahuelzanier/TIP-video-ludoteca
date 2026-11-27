import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createNews,
  deleteNews,
  getNews,
  getNewsPage,
  updateNews,
} from "./newsService";

let fetchMock: ReturnType<typeof vi.fn>;

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status });
}

function csrfResponse(): Response {
  return jsonResponse({ headerName: "X-XSRF-TOKEN", token: "csrf" });
}

function newsItem(id: number) {
  return {
    id,
    title: "News",
    description: "Description",
    publishedAt: "2026-01-01T10:00:00Z",
    authorUsername: "author",
    authorAvatarUrl: "/api/users/author/avatar",
    imageUrl: "/api/news/1/image",
  };
}

beforeEach(() => {
  fetchMock = vi.fn();
  vi.stubGlobal("fetch", fetchMock);
});

afterEach(() => vi.unstubAllGlobals());

describe("newsService", () => {
  it("loads a page and resolves asset URLs against the API origin", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({
      content: [newsItem(1)],
      page: 0,
      size: 9,
      totalElements: 1,
      totalPages: 1,
      hasNext: false,
      hasPrevious: false,
    }));

    const result = await getNewsPage(0, 9);

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/news?page=0&size=9",
      { credentials: "include", cache: "no-store" },
    );
    expect(result.content[0]).toMatchObject({
      imageUrl: "http://localhost:8080/api/news/1/image",
      authorAvatarUrl: "http://localhost:8080/api/users/author/avatar",
    });
  });

  it("reports when the feed requires login", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));
    await expect(getNewsPage()).rejects.toThrow(
      "Log in to see news from creators you follow.",
    );
  });

  it("loads a public detail and handles missing news", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse(newsItem(4)));
    await expect(getNews(4)).resolves.toMatchObject({ id: 4 });
    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/news/4",
      { credentials: "include", cache: "no-store" },
    );

    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "Not found" }, 404));
    await expect(getNews(99)).rejects.toThrow("Not found");
  });

  it("publishes with CSRF and multipart fields", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(jsonResponse(newsItem(1), 201));

    const image = new File(["image"], "photo.png", { type: "image/png" });
    await expect(createNews({ title: "  News  ", description: " Text ", image }))
      .resolves.toMatchObject({ id: 1 });

    const [url, options] = fetchMock.mock.calls[1];
    expect(url).toBe("http://localhost:8080/api/news");
    expect(options).toEqual(expect.objectContaining({
      method: "POST",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "csrf" },
    }));
    const body = options.body as FormData;
    expect(body.get("title")).toBe("News");
    expect(body.get("description")).toBe("Text");
    expect(body.get("image")).toBe(image);
  });

  it("updates only with the author's permission", async () => {
    fetchMock.mockResolvedValueOnce(csrfResponse());
    fetchMock.mockResolvedValueOnce(jsonResponse({ error: "Forbidden" }, 403));

    await expect(updateNews(1, { title: "News", description: "Text" }))
      .rejects.toThrow("Only the author can edit this news.");

    expect(fetchMock.mock.calls[1][0]).toBe("http://localhost:8080/api/news/1");
    expect(fetchMock.mock.calls[1][1].method).toBe("PUT");
  });

  it("deletes a news with CSRF and accepts 204", async () => {
    fetchMock
      .mockResolvedValueOnce(csrfResponse())
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    await expect(deleteNews(7)).resolves.toBeUndefined();

    expect(fetchMock.mock.calls[1][1]).toEqual(expect.objectContaining({
      method: "DELETE",
      credentials: "include",
      headers: { "X-XSRF-TOKEN": "csrf" },
    }));
  });
});
