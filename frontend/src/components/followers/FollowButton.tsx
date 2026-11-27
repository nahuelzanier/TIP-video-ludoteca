import { Link } from "react-router-dom";
import { UserCheck, UserPlus } from "lucide-react";
import { useTranslation } from "../../services/i18n";
import "./FollowButton.css";

interface FollowButtonProps {
  username: string;
  following: boolean;
  loading: boolean;
  error: string;
  isAuthenticated: boolean;
  onToggle: () => void;
}

function FollowButton({
  username,
  following,
  loading,
  error,
  isAuthenticated,
  onToggle,
}: FollowButtonProps) {
  const { t } = useTranslation();

  if (!isAuthenticated) {
    return (
      <Link className="follow-button follow-button--login" to="/login">
        {t("Log in to follow")}
      </Link>
    );
  }

  return (
    <div className="follow-button-group">
      <button
        className={`follow-button${following ? " follow-button--following" : ""}`}
        type="button"
        onClick={onToggle}
        disabled={loading}
        aria-pressed={following}
        aria-label={`${following ? t("Unfollow") : t("Follow")} ${username}`}
      >
        {following ? (
          <UserCheck size={16} aria-hidden="true" />
        ) : (
          <UserPlus size={16} aria-hidden="true" />
        )}
        {following ? t("Following") : t("Follow")}
      </button>

      {error && (
        <p className="follow-button__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}

export default FollowButton;
