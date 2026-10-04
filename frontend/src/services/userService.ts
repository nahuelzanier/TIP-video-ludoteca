import { API_BASE_URL, fetchCsrfToken, readError, resolveAssetUrl } from "./api";
import type { UserProfile } from "../types/User";
import type { GamePage } from "./gameService";

export const MAX_AVATAR_BYTES = 2 * 1024 * 1024;

function withResolvedAvatar(profile: UserProfile): UserProfile {
  return { ...profile, avatarUrl: resolveAssetUrl(profile.avatarUrl) };
}


export async function getUserProfile(
    username: string,
): Promise<UserProfile | null> {
    const response = await fetch(
        `${API_BASE_URL}/api/users/${encodeURIComponent(username)}/profile`,
    );

    if (response.status === 404) {
        return null;
    }

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const profile: UserProfile = await response.json();

    return withResolvedAvatar(profile);
}

export async function updateDescription(
    username: string,
    description: string,
): Promise<UserProfile> {
    const csrf = await fetchCsrfToken();

    const response = await fetch(
        `${API_BASE_URL}/api/users/${encodeURIComponent(username)}/description`,
        {
            method: "PATCH",
            credentials: "include",
            headers: {
                "Content-Type": "application/json",
                [csrf.headerName]: csrf.token,
            },
            body: JSON.stringify({ description }),
        },
    );

    if (response.status === 401) {
        throw new Error("Tu sesión expiró. Volvé a iniciar sesión.");
    }

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const profile: UserProfile = await response.json();

    return withResolvedAvatar(profile);
}

export async function uploadAvatar(
    username: string,
    avatar: File,
): Promise<UserProfile> {
    const csrf = await fetchCsrfToken();
    const formData = new FormData();
    formData.append("avatar", avatar);

    const response = await fetch(
        `${API_BASE_URL}/api/users/${encodeURIComponent(username)}/avatar`,
        {
            method: "PUT",
            credentials: "include",
            headers: {
                [csrf.headerName]: csrf.token,
            },
            body: formData,
        },
    );

    if (response.status === 401) {
        throw new Error("Tu sesión expiró. Volvé a iniciar sesión.");
    }

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const profile: UserProfile = await response.json();

    return withResolvedAvatar(profile);
}

export async function deleteAvatar(username: string): Promise<UserProfile> {
    const csrf = await fetchCsrfToken();

    const response = await fetch(
        `${API_BASE_URL}/api/users/${encodeURIComponent(username)}/avatar`,
        {
            method: "DELETE",
            credentials: "include",
            headers: {
                [csrf.headerName]: csrf.token,
            },
        },
    );

    if (response.status === 401) {
        throw new Error("Tu sesión expiró. Volvé a iniciar sesión.");
    }

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const profile: UserProfile = await response.json();

    return withResolvedAvatar(profile);
}

export async function getUserGamesPage(
    username: string,
    page = 0,
    size = 12,
): Promise<GamePage> {
    const params = new URLSearchParams({
        page: String(page),
        size: String(size),
    });

    const response = await fetch(
        `${API_BASE_URL}/api/users/${encodeURIComponent(username)}/games?${params}`,
    );

    if (!response.ok) {
        throw new Error(await readError(response));
    }

    const result: GamePage = await response.json();

    return {
        ...result,
        content: result.content.map((game) => ({
            ...game,
            image: new URL(game.image, API_BASE_URL).toString(),
        })),
    };
}