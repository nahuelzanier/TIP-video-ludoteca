import { API_BASE_URL } from "./api";

export interface TagOption {
  id: number;
  name: string;
  slug: string;
}

export interface GameTag {
  tagId: number;
  name: string;
  slug: string;
  assignmentCount: number;
  firstAssignedAt: string;
}

interface CsrfResponse {
  headerName: string;
  token: string;
}

export async function getMyGameTagIds(gameId: string): Promise<number[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/tags/mine`,
    {
      credentials: "include",
      cache: "no-store",
    },
  );

  if (!response.ok) {
    return readError(response);
  }

  return response.json();
}

async function readError(response: Response): Promise<never> {
  const text = await response.text();
  const fallbackMessage = `Request failed (${response.status}).`;
  let body: unknown;

  try {
    body = JSON.parse(text);
  } catch {
    throw new Error(text || fallbackMessage);
  }

  if (typeof body === "object" && body !== null) {
    const errorBody = body as Record<string, unknown>;
    const message = errorBody.detail ?? errorBody.message ?? errorBody.error;

    if (typeof message === "string" && message.length > 0) {
      throw new Error(message);
    }
  }

  throw new Error(text || fallbackMessage);
}

export async function getAvailableTags(): Promise<TagOption[]> {
  const response = await fetch(`${API_BASE_URL}/api/tags`);

  if (!response.ok) {
    return readError(response);
  }

  return response.json();
}

export async function getGameTags(gameId: string): Promise<GameTag[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/tags`,
  );

  if (!response.ok) {
    return readError(response);
  }

  return response.json();
}

export async function assignGameTag(
  gameId: string,
  tagId: number,
): Promise<GameTag[]> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!csrfResponse.ok) {
    throw new Error("Could not prepare the tag request.");
  }

  const csrf: CsrfResponse = await csrfResponse.json();

  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/tags`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        "Content-Type": "application/json",
        [csrf.headerName]: csrf.token,
      },
      body: JSON.stringify({ tagId }),
    },
  );

  if (!response.ok) {
    return readError(response);
  }

  return response.json();
}
