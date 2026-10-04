import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ProfileHeader from "../../components/profile/ProfileHeader";
import EditDescriptionModal from "../../components/profile/EditDescriptionModal";
import UserActivity from "../../components/profile/UserActivity";
import {
    getCurrentUser,
    logout as logoutUser,
    type AuthUser,
} from "../../services/authService";
import { getUserGamesPage, getUserProfile, updateDescription } from "../../services/userService";
import type { GamePage } from "../../services/gameService";
import type { ForumSummary, UserProfile } from "../../types/User";
import "./User.css";
import GameGrid from "../../components/games/GameGrid";



const MAX_DESCRIPTION_LENGTH = 500;

const FORUMS: ForumSummary[] = [];

interface ProfileState {
    username: string;
    profile: UserProfile | null;
    error: string;
    loaded: boolean;
}

const INITIAL_STATE: ProfileState = {
    username: "",
    profile: null,
    error: "",
    loaded: false,
};

function User() {
    const { username } = useParams<{ username: string }>();
    const navigate = useNavigate();
    const requestedUsername = username ?? "";

    const [state, setState] = useState<ProfileState>(INITIAL_STATE);
    const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
    const [isEditing, setIsEditing] = useState(false);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState("");
    const [loggingOut, setLoggingOut] = useState(false);
    const [logoutError, setLogoutError] = useState("");

    const [gamesPage, setGamesPage] = useState<GamePage | null>(null);
    const [gamesPageNumber, setGamesPageNumber] = useState(0);
    const [gamesLoading, setGamesLoading] = useState(true);
    const [gamesError, setGamesError] = useState("");

    useEffect(() => {
        getCurrentUser()
            .then(setCurrentUser)
            .catch(() => setCurrentUser(null));
    }, []);

    useEffect(() => {
        let cancelled = false;

        getUserProfile(requestedUsername)
            .then((data) => {
                if (!cancelled) {
                    setState({
                        username: requestedUsername,
                        profile: data,
                        error: "",
                        loaded: true,
                    });
                }
            })
            .catch((requestError: unknown) => {
                if (!cancelled) {
                    setState({
                        username: requestedUsername,
                        profile: null,
                        error:
                            requestError instanceof Error
                                ? requestError.message
                                : "No se pudo cargar el perfil.",
                        loaded: true,
                    });
                }
            });

        return () => {
            cancelled = true;
        };
    }, [requestedUsername]);

    const isLoading = !state.loaded || state.username !== requestedUsername;
    const profile = isLoading ? null : state.profile;
    const error = isLoading ? "" : state.error;
    const isOwnProfile = profile !== null && currentUser?.id === profile.id;

    useEffect(() => {
        let cancelled = false;

        setGamesLoading(true);
        setGamesError("");

        getUserGamesPage(requestedUsername, gamesPageNumber)
            .then((data) => {
                if (!cancelled) {
                    setGamesPage(data);
                }
            })
            .catch((requestError: unknown) => {
                if (!cancelled) {
                    setGamesError(
                        requestError instanceof Error
                            ? requestError.message
                            : "No se pudieron cargar los juegos.",
                    );
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setGamesLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [requestedUsername, gamesPageNumber]);

    async function handleSave(description: string) {
        if (!profile) {
            return;
        }

        setSaving(true);
        setSaveError("");

        try {
            const updated = await updateDescription(profile.username, description);

            setState((previous) => ({ ...previous, profile: updated }));
            setIsEditing(false);
        } catch (requestError) {
            setSaveError(
                requestError instanceof Error
                    ? requestError.message
                    : "No se pudo guardar la descripción.",
            );
        } finally {
            setSaving(false);
        }
    }

    async function handleLogout() {
        setLogoutError("");
        setLoggingOut(true);

        try {
            await logoutUser();
            navigate("/login", { replace: true });
        } catch (requestError) {
            setLogoutError(
                requestError instanceof Error
                    ? requestError.message
                    : "No se pudo cerrar sesión. Intentá de nuevo.",
            );
        } finally {
            setLoggingOut(false);
        }
    }

    if (isLoading) {
        return (
            <main className="user-page">
                <p className="user-status" role="status">
                    Cargando perfil...
                </p>
            </main>
        );
    }

    if (error) {
        return (
            <main className="user-page">
                <h1>No se pudo cargar el perfil</h1>

                <p className="user-status" role="alert">
                    {error}
                </p>

                <Link className="user-back" to="/search">
                    Volver a buscar
                </Link>
            </main>
        );
    }

    if (!profile) {
        return (
            <main className="user-page">
                <h1>Usuario no encontrado</h1>

                <p className="user-status">
                    No existe ningún usuario con el nombre {requestedUsername}.
                </p>

                <Link className="user-back" to="/search">
                    Buscar usuarios
                </Link>
            </main>
        );
    }

    return (
        <main className="user-page">
            <ProfileHeader
                username={profile.username}
                description={profile.description}
                isOwnProfile={isOwnProfile}
                loggingOut={loggingOut}
                error={logoutError}
                onEdit={() => {
                    setSaveError("");
                    setIsEditing(true);
                }}
                onUpload={() => navigate("/games/upload")}
                onLogout={handleLogout}
            />

            <UserActivity forums={FORUMS} />

            <section className="user-games">
    <h2>Juegos publicados</h2>

    {gamesLoading && <p role="status">Cargando juegos...</p>}
    {gamesError && <p role="alert">{gamesError}</p>}

    {!gamesLoading &&
        !gamesError &&
        gamesPage?.content.length === 0 && (
            <p>Este usuario todavía no publicó juegos.</p>
        )}

    {gamesPage && gamesPage.content.length > 0 && (
        <>
            <GameGrid games={gamesPage.content} />

            {gamesPage.totalPages > 1 && (
                <nav className="user-games-pagination" aria-label="Páginas de juegos">
                    <button
                        type="button"
                        onClick={() => setGamesPageNumber((page) => page - 1)}
                        disabled={!gamesPage.hasPrevious || gamesLoading}
                    >
                        Anterior
                    </button>

                    <span>
                        Página {gamesPage.page + 1} de {gamesPage.totalPages}
                    </span>

                    <button
                        type="button"
                        onClick={() => setGamesPageNumber((page) => page + 1)}
                        disabled={!gamesPage.hasNext || gamesLoading}
                    >
                        Siguiente
                    </button>
                </nav>
            )}
        </>
    )}
</section>

            {isEditing && (
                <EditDescriptionModal
                    initialDescription={profile.description ?? ""}
                    maxLength={MAX_DESCRIPTION_LENGTH}
                    saving={saving}
                    error={saveError}
                    onSave={handleSave}
                    onClose={() => {
                        setIsEditing(false);
                        setSaveError("");
                    }}
                />
            )}
        </main>
    );
}

export default User;
