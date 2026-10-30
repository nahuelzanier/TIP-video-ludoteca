import { API_BASE_URL, readError } from "./api";

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface GameUploadResult {
  id: string;
  title: string;
  filesStored: number;
}

export interface GamePublishResult {
  id: string;
  title: string;
  status: string;
}

export async function uploadGame(
  title: string,
  description: string,
  cover: File,
  archive: File,
): Promise<GameUploadResult> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!csrfResponse.ok) {
    throw new Error("Could not prepare the upload request.");
  }

  const csrf: CsrfResponse = await csrfResponse.json();
  const formData = new FormData();
  formData.append("title", title);
  formData.append("description", description);
  formData.append("cover", cover);
  formData.append("archive", archive);

  const response = await fetch(`${API_BASE_URL}/api/games`, {
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

export async function publishGame(gameId: string): Promise<GamePublishResult> {
  const csrfResponse = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!csrfResponse.ok) {
    throw new Error("Could not prepare the publish request.");
  }

  const csrf: CsrfResponse = await csrfResponse.json();

  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/publish`,
    {
      method: "POST",
      credentials: "include",
      headers: {
        [csrf.headerName]: csrf.token,
      },
    },
  );

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  return response.json();
}
