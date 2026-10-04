import { useState } from "react";
import { Link } from "react-router-dom";
import { resolveAssetUrl } from "../../services/api";
import StarRating from "./StarRating";
import "./UserBadge.css";

interface UserBadgeProps {
  username: string;
  profileImageUrl: string | null;
  createdAt: string;
  isGameAuthor: boolean;
  rating?: number | null;
  avatarSize?: "small" | "regular";
}

function UserBadge({
  username,
  profileImageUrl,
  createdAt,
  isGameAuthor,
  rating = null,
  avatarSize = "regular",
}: UserBadgeProps) {
  const profilePath = `/user/${encodeURIComponent(username)}`;
  const avatarClass =
    avatarSize === "small" ? "user-badge__avatar--small" : "user-badge__avatar";
  const [failedUrl, setFailedUrl] = useState<string | null>(null);

  const avatarUrl = resolveAssetUrl(profileImageUrl);

  const showImage = Boolean(avatarUrl) && failedUrl !== avatarUrl;

  return (
    <div className="user-badge">
      <Link className={avatarClass} to={profilePath} aria-hidden="true" tabIndex={-1}>
        {showImage ? (
          <img
            src={avatarUrl ?? undefined}
            alt=""
            loading="lazy"
            onError={() => setFailedUrl(avatarUrl)}
          />
        ) : (
          username.charAt(0).toUpperCase()
        )}
      </Link>

      <div className="user-badge__body">
        <div className="user-badge__line">
          <Link className="user-badge__name" to={profilePath}>
            {username}
          </Link>

          {isGameAuthor && <span className="user-badge__author">(autor)</span>}

          {rating !== null && <StarRating value={rating} />}
        </div>

        <time className="user-badge__date" dateTime={createdAt}>
          {formatDate(createdAt)}
        </time>
      </div>
    </div>
  );
}

function formatDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleDateString("es-AR", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

export default UserBadge;