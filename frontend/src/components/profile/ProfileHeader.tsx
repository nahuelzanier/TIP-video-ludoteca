import "./ProfileHeader.css";

const EMPTY_DESCRIPTION = "Este usuario aún no agregó una descripción";

interface ProfileHeaderProps {
    username: string;
    description: string | null;
    isOwnProfile: boolean;
    loggingOut: boolean;
    error: string;
    onEdit: () => void;
    onLogout: () => void;
}

function ProfileHeader({
    username,
    description,
    isOwnProfile,
    loggingOut,
    error,
    onEdit,
    onLogout,
}: ProfileHeaderProps) {
    const hasDescription = Boolean(description && description.trim());

    return (
        <header className="profile-header">
            <span className="profile-avatar" aria-hidden="true">
                {username.charAt(0).toUpperCase()}
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
