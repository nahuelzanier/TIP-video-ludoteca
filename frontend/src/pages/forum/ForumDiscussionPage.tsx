import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useParams } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import {
  createForumReply,
  getForumDiscussion,
  getForumReplies,
  rateForumDiscussion,
  type ForumDiscussionDetail,
  type ForumReply,
} from "../../services/forumService";
import "./ForumDiscussionPage.css";

function ForumDiscussionPage() {
  const { discussionId: rawId } = useParams();
  const location = useLocation();
  const discussionId = Number(rawId);

  const [discussion, setDiscussion] = useState<ForumDiscussionDetail | null>(null);
  const [replies, setReplies] = useState<ForumReply[]>([]);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [ratingError, setRatingError] = useState("");
  const [replyError, setReplyError] = useState("");
  const [ratingSaving, setRatingSaving] = useState(false);
  const [replySaving, setReplySaving] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);
  const [replyTargetId, setReplyTargetId] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState("");

  useEffect(() => {
    if (!Number.isInteger(discussionId) || discussionId <= 0) {
      setError("Discussion not found.");
      setLoading(false);
      return;
    }

    let isCurrentRequest = true;
    setLoading(true);
    setError("");

    Promise.all([
      getForumDiscussion(discussionId),
      getForumReplies(discussionId),
      getCurrentUser().catch(() => null),
    ])
      .then(([loadedDiscussion, loadedReplies, currentUser]) => {
        if (!isCurrentRequest) return;
        setDiscussion(loadedDiscussion);
        setReplies(loadedReplies);
        setUser(currentUser);
      })
      .catch((requestError: unknown) => {
        if (!isCurrentRequest) return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load this discussion.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [discussionId]);

  async function handleRating(rating: number) {
    if (!discussion || !user) return;

    setRatingError("");
    setRatingSaving(true);

    try {
      const result = await rateForumDiscussion(discussion.id, rating);
      setDiscussion((current) =>
        current
          ? {
              ...current,
              myRating: result.myRating,
              averageRating: result.averageRating,
              ratingCount: result.ratingCount,
            }
          : current,
      );
    } catch (requestError) {
      setRatingError(
        requestError instanceof Error
          ? requestError.message
          : "Could not save your rating.",
      );
    } finally {
      setRatingSaving(false);
    }
  }

  function startReply(parentReplyId: number | null = null) {
    setReplyTargetId(parentReplyId);
    setReplyContent("");
    setReplyError("");
    setShowReplyForm(true);
  }

  async function handleReplySubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!discussion || !replyContent.trim()) return;

    setReplyError("");
    setReplySaving(true);

    try {
      await createForumReply(
        discussion.id,
        replyContent.trim(),
        replyTargetId,
      );
      setReplies(await getForumReplies(discussion.id));
      setReplyContent("");
      setReplyTargetId(null);
      setShowReplyForm(false);
    } catch (requestError) {
      setReplyError(
        requestError instanceof Error
          ? requestError.message
          : "Could not post your reply.",
      );
    } finally {
      setReplySaving(false);
    }
  }

  if (loading) {
    return (
      <main className="forum-detail-page">
        <p role="status">Loading discussion...</p>
      </main>
    );
  }

  if (error || !discussion) {
    return (
      <main className="forum-detail-page">
        <Link to="/forum">← Back to forum</Link>
        <p className="forum-detail-error" role="alert">
          {error || "Discussion not found."}
        </p>
      </main>
    );
  }

  const replyTarget = replies.find((reply) => reply.id === replyTargetId);

  return (
    <main className="forum-detail-page">
      <Link className="forum-detail-back" to="/forum">
        ← Back to forum
      </Link>

      <article className="forum-detail">
        <img
          className="forum-detail__cover"
          src={discussion.coverImageUrl}
          alt=""
        />

        <div className="forum-detail__content">
          <p className="forum-detail__game">
            Discussion about{" "}
            <Link to={`/game/${encodeURIComponent(discussion.gameId)}`}>
              {discussion.gameTitle}
            </Link>
          </p>

          <h1>{discussion.title}</h1>

          <p className="forum-detail__author">
            By{" "}
            <Link to={`/user/${encodeURIComponent(discussion.authorUsername)}`}>
              {discussion.authorUsername}
            </Link>
            {" · "}
            {new Date(discussion.createdAt).toLocaleDateString()}
          </p>

          <p className="forum-detail__description">{discussion.description}</p>

          <ul className="forum-detail__tags" aria-label="Discussion tags">
            {discussion.tags.map((tag) => (
              <li key={tag.id}>{tag.name}</li>
            ))}
          </ul>

          <section className="forum-rating" aria-label="Discussion rating">
            <h2>Rating</h2>
            <p>
              {discussion.averageRating === null
                ? "No ratings yet"
                : `${discussion.averageRating.toFixed(1)} out of 5`}
              {" · "}
              {discussion.ratingCount}{" "}
              {discussion.ratingCount === 1 ? "rating" : "ratings"}
            </p>

            {user ? (
              <div className="forum-rating__buttons" aria-label="Rate this discussion">
                {[1, 2, 3, 4, 5].map((score) => (
                  <button
                    key={score}
                    type="button"
                    aria-label={`Rate ${score} out of 5`}
                    aria-pressed={discussion.myRating === score}
                    disabled={ratingSaving}
                    onClick={() => handleRating(score)}
                  >
                    {score <= (discussion.myRating ?? 0) ? "★" : "☆"}
                  </button>
                ))}
              </div>
            ) : (
              <p>
                <Link to="/login" state={{ from: location.pathname }}>
                  Sign in
                </Link>{" "}
                to rate this discussion.
              </p>
            )}

            {ratingError && (
              <p className="forum-detail-error" role="alert">
                {ratingError}
              </p>
            )}
          </section>
        </div>
      </article>

      <section className="forum-replies" aria-labelledby="forum-replies-title">
        <div className="forum-replies__header">
          <h2 id="forum-replies-title">Replies ({replies.length})</h2>
          {user ? (
            <button type="button" onClick={() => startReply()}>
              Add a reply
            </button>
          ) : (
            <p>
              <Link to="/login" state={{ from: location.pathname }}>
                Sign in
              </Link>{" "}
              to reply.
            </p>
          )}
        </div>

        {replies.length === 0 && (
          <p className="forum-replies__empty">
            No replies yet. Start the conversation.
          </p>
        )}

        <div className="forum-replies__list">
          {replies.map((reply) => (
            <article className="forum-reply" key={reply.id}>
              <p className="forum-reply__meta">
                <strong>{reply.authorUsername}</strong>
                {" · "}
                {new Date(reply.createdAt).toLocaleString()}
                {reply.parentReplyId !== null &&
                  ` · Reply to #${reply.parentReplyId}`}
              </p>
              <p className="forum-reply__content">{reply.content}</p>
              {user && (
                <button
                  className="forum-reply__button"
                  type="button"
                  onClick={() => startReply(reply.id)}
                >
                  Reply
                </button>
              )}
            </article>
          ))}
        </div>

        {showReplyForm && user && (
          <form className="forum-reply-form" onSubmit={handleReplySubmit}>
            <label htmlFor="forum-reply-content">
              {replyTarget
                ? `Replying to ${replyTarget.authorUsername}`
                : "Your reply"}
            </label>
            <textarea
              id="forum-reply-content"
              value={replyContent}
              onChange={(event) => setReplyContent(event.target.value)}
              maxLength={3000}
              rows={5}
              required
            />

            {replyError && (
              <p className="forum-detail-error" role="alert">
                {replyError}
              </p>
            )}

            <div className="forum-reply-form__actions">
              <button type="submit" disabled={replySaving}>
                {replySaving ? "Posting..." : "Post reply"}
              </button>
              <button
                type="button"
                onClick={() => setShowReplyForm(false)}
                disabled={replySaving}
              >
                Cancel
              </button>
            </div>
          </form>
        )}
      </section>
    </main>
  );
}

export default ForumDiscussionPage;