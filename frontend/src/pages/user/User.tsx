import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import ProfileHeader from "../../components/profile/ProfileHeader";
import AvatarModal from "../../components/profile/AvatarModal";
import EditDescriptionModal from "../../components/profile/EditDescriptionModal";
import UserActivity from "../../components/profile/UserActivity";
import {
    getCurrentUser,
    logout as logoutUser,
    type AuthUser,
} from "../../services/authService";
import {
    deleteAvatar,
    getUserGamesPage,
    getUserProfile,
    updateDescription,
    uploadAvatar,
} from "../../services/userService";
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

    const [isAvatarOpen, setIsAvatarOpen] = useState(false);
    const [avatarSaving, setAvatarSaving] = useState(false);
    const [avatarRemoving, setAvatarRemoving] = useState(false);
    const [avatarError, setAvatarError] = useState("");

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
                                : "Could not load the profile.",
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

        // eslint-disable-next-line react-hooks/set-state-in-effect -- page and username changes start a distinct profile-games request.
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
                            : "Could not load the games.",
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
                    : "Could not save the description.",
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
                    : "Could not log out. Please try again.",
            );
        } finally {
            setLoggingOut(false);
        }
    }

    async function handleAvatarSave(file: File) {
        if (!profile) {
            return;
        }

        setAvatarSaving(true);
        setAvatarError("");

        try {
            const updated = await uploadAvatar(profile.username, file);

            setState((previous) => ({ ...previous, profile: updated }));
            setIsAvatarOpen(false);
        } catch (requestError) {
            setAvatarError(
                requestError instanceof Error
                    ? requestError.message
                    : "Could not save the picture.",
            );
        } finally {
            setAvatarSaving(false);
        }
    }

    async function handleAvatarRemove() {
        if (!profile) {
            return;
        }

        setAvatarRemoving(true);
        setAvatarError("");

        try {
            const updated = await deleteAvatar(profile.username);

            setState((previous) => ({ ...previous, profile: updated }));
            setIsAvatarOpen(false);
        } catch (requestError) {
            setAvatarError(
                requestError instanceof Error
                    ? requestError.message
                    : "Could not remove the picture.",
            );
        } finally {
            setAvatarRemoving(false);
        }
    }

    if (isLoading) {
        return (
            <main className="user-page">
                <p className="user-status" role="status">
                    Loading profile...
                </p>
            </main>
        );
    }

    if (error) {
        return (
            <main className="user-page">
                <h1>Could not load the profile</h1>

                <p className="user-status" role="alert">
                    {error}
                </p>

                <Link className="user-back" to="/search">
                    Back to search
                </Link>
            </main>
        );
    }

    if (!profile) {
        return (
            <main className="user-page">
                <h1>User not found</h1>

                <p className="user-status">
                    No user named {requestedUsername} exists.
                </p>

                <Link className="user-back" to="/search">
                    Search for users
                </Link>
            </main>
        );
    }

    return (
        <main className="user-page">
            <ProfileHeader
                username={profile.username}
                description={profile.description}
                avatarUrl={profile.avatarUrl}
                isOwnProfile={isOwnProfile}
                loggingOut={loggingOut}
                error={logoutError}
                onEdit={() => {
                    setSaveError("");
                    setIsEditing(true);
                }}
                onChangePhoto={() => {
                    setAvatarError("");
                    setIsAvatarOpen(true);
                }}
                onUpload={() => navigate("/games/upload")}
                onLogout={handleLogout}
            />

            <UserActivity forums={FORUMS} />

            {!gamesLoading && !gamesError && (gamesPage?.totalElements ?? 0) > 0 && (
                <section className="user-games">
                    <h2>{isOwnProfile ? "My games" : "Published games"}</h2>

                    {gamesPage && gamesPage.content.length > 0 && (
                        <>
                            <GameGrid games={gamesPage.content} />

                            {gamesPage.totalPages > 1 && (
                                <nav className="user-games-pagination" aria-label="Game pages">
                                    <button
                                        type="button"
                                        onClick={() => setGamesPageNumber((page) => page - 1)}
                                        disabled={!gamesPage.hasPrevious || gamesLoading}
                                    >
                                        Previous
                                    </button>

                                    <span>
                                        Page {gamesPage.page + 1} of {gamesPage.totalPages}
                                    </span>

                                    <button
                                        type="button"
                                        onClick={() => setGamesPageNumber((page) => page + 1)}
                                        disabled={!gamesPage.hasNext || gamesLoading}
                                    >
                                        Next
                                    </button>
                                </nav>
                            )}
                        </>
                    )}
                </section>
            )}

            {isAvatarOpen && (
                <AvatarModal
                    username={profile.username}
                    currentAvatarUrl={profile.avatarUrl}
                    saving={avatarSaving}
                    removing={avatarRemoving}
                    error={avatarError}
                    onSave={handleAvatarSave}
                    onRemove={handleAvatarRemove}
                    onClose={() => {
                        setIsAvatarOpen(false);
                        setAvatarError("");
                    }}
                />
            )}

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
