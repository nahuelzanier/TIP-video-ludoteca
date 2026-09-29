import { API_BASE_URL } from "./api";

interface CsrfResponse {
  headerName: string;
  token: string;
}

export interface GameUploadResult {
  id: string;
  title: string;
  filesStored: number;
}

export async function uploadGame(
  title: string,
  description: string,
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
    const text = await response.text();

    try {
      const error = JSON.parse(text);
      throw new Error(
        error.detail ?? error.message ?? error.error ?? "Game upload failed.",
      );
    } catch (error) {
      if (error instanceof Error && error.message !== "Game upload failed.") {
        throw error;
      }

      throw new Error(text || `Upload failed with status ${response.status}.`);
    }
  }

  return response.json();
}