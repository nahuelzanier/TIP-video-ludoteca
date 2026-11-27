import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import type { FollowCounts, FollowStatus } from "../types/News";

function encodedUsername(username: string): string {
  return encodeURIComponent(username.trim());
}

export async function getFollowStatus(username: string): Promise<FollowStatus> {
  const response = await fetch(
    `${API_BASE_URL}/api/follows/${encodedUsername(username)}/status`,
    { credentials: "include", cache: "no-store" },
  );

  if (response.status === 401) {
    return { username, following: false };
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export async function getFollowCounts(username: string): Promise<FollowCounts> {
  const response = await fetch(
    `${API_BASE_URL}/api/follows/${encodedUsername(username)}/counts`,
    { cache: "no-store" },
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}

export async function followUser(username: string): Promise<boolean> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/follows/${encodedUsername(username)}`,
    {
      method: "PUT",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    },
  );

  if (response.status === 401) {
    throw new Error("Log in to follow creators.");
  }

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  const result: FollowStatus = await response.json();
  return result.following;
}

export async function unfollowUser(username: string): Promise<boolean> {
  const csrf = await fetchCsrfToken();

  const response = await fetch(
    `${API_BASE_URL}/api/follows/${encodedUsername(username)}`,
    {
      method: "DELETE",
      credentials: "include",
      headers: { [csrf.headerName]: csrf.token },
    },
  );

  if (response.status === 401) {
    throw new Error("Log in to unfollow creators.");
  }

  if (!response.ok && response.status !== 204) {
    throw new Error(await readError(response));
  }

  return false;
}
