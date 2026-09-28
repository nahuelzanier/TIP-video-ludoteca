import { API_BASE_URL, fetchCsrfToken, readError } from "./api";
import type { UserProfile } from "../types/User";

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

    return response.json();
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

    return response.json();
}
