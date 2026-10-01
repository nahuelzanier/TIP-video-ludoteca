import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import type { Comment, CreateCommentPayload } from "../types/Comment";

export const MAX_COMMENT_LENGTH = 1000;

async function postComment<T>(url: string, body: Record<string, unknown>): Promise<T> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(url, {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  });

  if (response.status === 401) {
    throw new Error("Tu sesión expiró. Volvé a iniciar sesión.");
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export async function getGameComments(gameId: string): Promise<Comment[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/comments`,
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export function createComment(
  gameId: string,
  payload: CreateCommentPayload,
): Promise<Comment> {
  return postComment<Comment>(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/comments`,
    payload as unknown as Record<string, unknown>,
  );
}

export function createReply(
  commentId: number,
  content: string,
): Promise<Comment> {
  return postComment<Comment>(
    `${API_BASE_URL}/api/comments/${commentId}/replies`,
    { content },
  );
}