import {
  API_BASE_URL,
  fetchCsrfToken,
  readError,
  resolveAssetUrl,
} from "./api";

export interface ForumTag {
  id: number;
  name: string;
  slug: string;
}

export async function getForumTags(): Promise<ForumTag[]> {
  const response = await fetch(API_BASE_URL + "/api/forum/tags");

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}
export interface ForumDiscussion {
  id: number;
  title: string;
  description: string;
  createdAt: string;
  gameId: string;
  gameTitle: string;
  authorUsername: string;
  authorAvatarUrl: string | null;
  coverImageUrl: string;
  tags: ForumTag[];
  averageRating: number | null;
  ratingCount: number;
}

export interface ForumDiscussionPage {
  content: ForumDiscussion[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export async function getForumDiscussions(
  page = 0,
  size = 12,
): Promise<ForumDiscussionPage> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  const response = await fetch(
    `${API_BASE_URL}/api/forum/discussions?${params.toString()}`,
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const result: ForumDiscussionPage = await response.json();

  return {
    ...result,
    content: result.content.map((discussion) => ({
      ...discussion,
      coverImageUrl:
        resolveAssetUrl(discussion.coverImageUrl) ?? discussion.coverImageUrl,
      authorAvatarUrl: resolveAssetUrl(discussion.authorAvatarUrl),
    })),
  };
}

export interface ForumDiscussionDetail extends ForumDiscussion {
  myRating: number | null;
}

export interface ForumReply {
  id: number;
  content: string;
  createdAt: string;
  authorUsername: string;
  authorAvatarUrl: string | null;
  parentReplyId: number | null;
}

export interface ForumRatingResult {
  discussionId: number;
  myRating: number;
  averageRating: number;
  ratingCount: number;
}

export async function getForumDiscussion(
  discussionId: number,
): Promise<ForumDiscussionDetail> {
  const response = await fetch(
    `${API_BASE_URL}/api/forum/discussions/${discussionId}`,
    { credentials: "include" },
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const discussion: ForumDiscussionDetail = await response.json();

  return {
    ...discussion,
    coverImageUrl:
      resolveAssetUrl(discussion.coverImageUrl) ?? discussion.coverImageUrl,
    authorAvatarUrl: resolveAssetUrl(discussion.authorAvatarUrl),
  };
}

export async function getForumReplies(
  discussionId: number,
): Promise<ForumReply[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/forum/discussions/${discussionId}/replies`,
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const replies: ForumReply[] = await response.json();

  return replies.map((reply) => ({
    ...reply,
    authorAvatarUrl: resolveAssetUrl(reply.authorAvatarUrl),
  }));
}

async function sendForumRequest<T>(
  url: string,
  method: "POST" | "PUT",
  body: Record<string, unknown>,
): Promise<T> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(url, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      [csrf.headerName]: csrf.token,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export function createForumReply(
  discussionId: number,
  content: string,
  parentReplyId: number | null = null,
): Promise<ForumReply> {
  return sendForumRequest<ForumReply>(
    `${API_BASE_URL}/api/forum/discussions/${discussionId}/replies`,
    "POST",
    { content, parentReplyId },
  );
}

export function rateForumDiscussion(
  discussionId: number,
  rating: number,
): Promise<ForumRatingResult> {
  return sendForumRequest<ForumRatingResult>(
    `${API_BASE_URL}/api/forum/discussions/${discussionId}/rating`,
    "PUT",
    { rating },
  );
}

export interface CreateForumDiscussionResult {
  id: number;
  title: string;
}

export async function createForumDiscussion(input: {
  gameId: string;
  title: string;
  description: string;
  cover: File | null;
  tagIds: number[];
}): Promise<CreateForumDiscussionResult> {
  const csrf = await fetchCsrfToken();
  const formData = new FormData();

  formData.append("gameId", input.gameId);
  formData.append("title", input.title);
  formData.append("description", input.description);
  if (input.cover) {
    formData.append("cover", input.cover);
  }

  for (const tagId of input.tagIds) {
    formData.append("tagIds", String(tagId));
  }

  const response = await fetch(`${API_BASE_URL}/api/forum/discussions`, {
    method: "POST",
    credentials: "include",
    headers: {
      [csrf.headerName]: csrf.token,
    },
    body: formData,
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}
