import { useState } from "react";
import "./ProfileHeader.css";

const EMPTY_DESCRIPTION = "Este usuario aún no agregó una descripción";

interface ProfileHeaderProps {
    username: string;
    description: string | null;
    avatarUrl: string | null;
    isOwnProfile: boolean;
    loggingOut: boolean;
    error: string;
    onEdit: () => void;
    onChangePhoto: () => void;
    onUpload: () => void;
    onLogout: () => void;
}

function ProfileHeader({
    username,
    description,
    avatarUrl,
    isOwnProfile,
    loggingOut,
    error,
    onEdit,
    onChangePhoto,
    onUpload,
    onLogout,
}: ProfileHeaderProps) {
    const hasDescription = Boolean(description && description.trim());
    const [failedUrl, setFailedUrl] = useState<string | null>(null);

    const showImage = Boolean(avatarUrl) && failedUrl !== avatarUrl;

    return (
        <header className="profile-header">
            <span className="profile-avatar" aria-hidden="true">
                {showImage ? (
                    <img
                        src={avatarUrl ?? undefined}
                        alt=""
                        onError={() => setFailedUrl(avatarUrl)}
                    />
                ) : (
                    username.charAt(0).toUpperCase()
                )}
            </span>

            <div className="profile-header-body">
                <h1 className="profile-header-name">{username}</h1>

                <p
                    className={`profile-description${
                        hasDescription ? "" : " profile-description--empty"
                    }`}
                >
                    {hasDescription ? description : EMPTY_DESCRIPTION}
                </p>
            </div>

            {isOwnProfile && (
                <div className="profile-actions">
                    <button className="profile-edit" type="button" onClick={onEdit}>
                        Editar descripción
                    </button>

                    <button
                        className="profile-photo"
                        type="button"
                        onClick={onChangePhoto}
                    >
                        Cambiar foto
                    </button>

                    <button className="profile-upload" type="button" onClick={onUpload}>
                        Subir un juego
                    </button>

                    <button
                        className="profile-logout"
                        type="button"
                        onClick={onLogout}
                        disabled={loggingOut}
                    >
                        {loggingOut ? "Cerrando sesión..." : "Cerrar sesión"}
                    </button>
                </div>
            )}

            {isOwnProfile && error && (
                <p className="profile-error" role="alert">
                    {error}
                </p>
            )}
        </header>
    );
}

export default ProfileHeader;