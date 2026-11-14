import { useState } from "react";
import { Link } from "react-router-dom";
import { Pencil, Camera, LogOut, Settings } from "lucide-react";
import { useTranslation } from "../../services/i18n";
import "./ProfileHeader.css";

const EMPTY_DESCRIPTION = "This user hasn't added a description yet.";

interface ProfileHeaderProps {
    username: string;
    description: string | null;
    avatarUrl: string | null;
    isOwnProfile: boolean;
    isAdmin: boolean;
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
    isAdmin,
    loggingOut,
    error,
    onEdit,
    onChangePhoto,
    onUpload,
    onLogout,
}: ProfileHeaderProps) {
    const { t } = useTranslation();
    const hasDescription = Boolean(description && description.trim());
    const [failedUrl, setFailedUrl] = useState<string | null>(null);

    const showImage = Boolean(avatarUrl) && failedUrl !== avatarUrl;

    return (
        <header className="profile-header">
            <div className="profile-avatar-container">
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

                {isOwnProfile && (
                    <button
                        className="profile-avatar-edit"
                        type="button"
                        onClick={onChangePhoto}
                            aria-label={t("Change profile picture")}
                            title={t("Change profile picture")}
                    >
                        <Camera size={18} aria-hidden="true" />
                    </button>
                )}
            </div>

            <div className="profile-header-body">
                <h1 className="profile-header-name">{username}</h1>

                <div className="profile-description-header">
                    <h2 className="profile-description-title">{t("About")}</h2>
                    {isOwnProfile && (
                        <button
                            className="profile-description-edit"
                            type="button"
                            onClick={onEdit}
                            aria-label={t("Edit description")}
                            title={t("Edit description")}
                        >
                            <Pencil size={16} aria-hidden="true" />
                        </button>
                    )}
                </div>

                <p
                    className={`profile-description${
                        hasDescription ? "" : " profile-description--empty"
                    }`}
                >
                    {hasDescription ? description : t(EMPTY_DESCRIPTION)}
                </p>
            </div>

            {isOwnProfile && (
                <div className="profile-actions">
                    <button className="profile-upload" type="button" onClick={onUpload}>
                        {t("Upload a game")}
                    </button>

                    <Link className="profile-settings" to="/settings">
                        <Settings size={16} aria-hidden="true" />
                        Settings
                    </Link>

                    {isAdmin && (
                        <Link className="profile-admin-review" to="/admin/review">
                            {t("Review games")}
                        </Link>
                    )}

                    <button
                        className="profile-logout"
                        type="button"
                        onClick={onLogout}
                        disabled={loggingOut}
                        aria-label={t("Log out")}
                        title={t("Log out")}
                    >
                        <LogOut size={18} aria-hidden="true" />
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
