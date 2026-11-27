import { API_BASE_URL, fetchCsrfToken, readError, resolveAssetUrl } from "./api";
import type { NewsItem, NewsPage, NewsPayload } from "../types/News";

export const MAX_NEWS_TITLE_LENGTH = 150;
export const MAX_NEWS_DESCRIPTION_LENGTH = 3000;
export const MAX_NEWS_IMAGE_BYTES = 5 * 1024 * 1024;

function normalizeNews(item: NewsItem): NewsItem {
  return {
    ...item,
    authorAvatarUrl: resolveAssetUrl(item.authorAvatarUrl),
    imageUrl: resolveAssetUrl(item.imageUrl),
  };
}

async function getJson<T>(url: string, unauthorizedMessage: string): Promise<T> {
  const response = await fetch(url, { credentials: "include", cache: "no-store" });

  if (response.status === 401) {
    throw new Error(unauthorizedMessage);
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export async function getNewsPage(page = 0, size = 9): Promise<NewsPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const result = await getJson<NewsPage>(
    `${API_BASE_URL}/api/news?${params}`,
    "Log in to see news from creators you follow.",
  );

  return {
    ...result,
    content: result.content.map(normalizeNews),
  };
}

export function getNews(noticiaId: number): Promise<NewsItem> {
  return getJson<NewsItem>(
    `${API_BASE_URL}/api/news/${noticiaId}`,
    "Could not load this news.",
  ).then(normalizeNews);
}

function toFormData(payload: NewsPayload): FormData {
  const form = new FormData();
  form.append("title", payload.title.trim());
  form.append("description", payload.description.trim());

  if (payload.image) {
    form.append("image", payload.image);
  }

  return form;
}

async function sendNews(
  url: string,
  method: "POST" | "PUT",
  payload: NewsPayload,
  unauthorizedMessage: string,
): Promise<NewsItem> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(url, {
    method,
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
    body: toFormData(payload),
  });

  if (response.status === 401) {
    throw new Error(unauthorizedMessage);
  }

  if (response.status === 403) {
    throw new Error("Only the author can edit this news.");
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const result: NewsItem = await response.json();
  return normalizeNews(result);
}

export function createNews(payload: NewsPayload): Promise<NewsItem> {
  return sendNews(
    `${API_BASE_URL}/api/news`,
    "POST",
    payload,
    "Log in to publish a news.",
  );
}

export function updateNews(noticiaId: number, payload: NewsPayload): Promise<NewsItem> {
  return sendNews(
    `${API_BASE_URL}/api/news/${noticiaId}`,
    "PUT",
    payload,
    "Log in to edit this news.",
  );
}

export async function deleteNews(noticiaId: number): Promise<void> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(`${API_BASE_URL}/api/news/${noticiaId}`, {
    method: "DELETE",
    credentials: "include",
    headers: { [csrf.headerName]: csrf.token },
  });

  if (response.status === 401) {
    throw new Error("Log in to delete this news.");
  }

  if (response.status === 403) {
    throw new Error("Only the author can delete this news.");
  }

  if (!response.ok && response.status !== 204) {
    throw new Error(await readError(response));
  }
}
