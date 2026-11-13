import { Link } from "react-router-dom";
import type { ForumDiscussion } from "../../services/forumService";
import "./ForumDiscussionCard.css";

interface ForumDiscussionCardProps {
  discussion: ForumDiscussion;
}

function ForumDiscussionCard({ discussion }: ForumDiscussionCardProps) {
  return (
    <Link className="forum-card" to={`/forum/${discussion.id}`}>
      {discussion.coverImageUrl ? (
        <>
          <img
            className="forum-card__cover"
            src={discussion.coverImageUrl}
            alt=""
            loading="lazy"
            onError={(event) => {
              event.currentTarget.hidden = true;
              event.currentTarget.nextElementSibling?.removeAttribute("hidden");
            }}
          />
          <div className="forum-card__cover forum-card__cover-fallback" hidden aria-hidden="true">
            No cover image
          </div>
        </>
      ) : (
        <div className="forum-card__cover forum-card__cover-fallback" aria-hidden="true">
          No cover image
        </div>
      )}
      <div className="forum-card__body">
        <div className="forum-card__author">
          {discussion.authorAvatarUrl ? (
            <img className="forum-avatar" src={discussion.authorAvatarUrl} alt="" loading="lazy" />
          ) : (
            <span className="forum-avatar forum-avatar--fallback">
              {discussion.authorUsername.charAt(0).toUpperCase()}
            </span>
          )}
          <span>{discussion.authorUsername}</span>
          <time dateTime={discussion.createdAt}>
            {new Date(discussion.createdAt).toLocaleDateString("en-US")}
          </time>
        </div>
        <h2>{discussion.title}</h2>
        <p className="forum-card__description">{discussion.description}</p>
        <p className="forum-card__game">Game: <strong>{discussion.gameTitle}</strong></p>
        <ul className="forum-tags" aria-label="Discussion tags">
          {discussion.tags.map((tag) => <li key={tag.id}>{tag.name}</li>)}
        </ul>
        <p className="forum-card__rating">
          {discussion.averageRating === null
            ? "No ratings yet"
            : `${discussion.averageRating.toFixed(1)} / 5`}
          {" · "}{discussion.ratingCount} {discussion.ratingCount === 1 ? "rating" : "ratings"}
        </p>
      </div>
    </Link>
  );
}

export default ForumDiscussionCard;
