import { useEffect, useState } from "react";
import type { ForumDiscussion } from "../../services/forumService";
import { getForumDiscussions } from "../../services/forumService";
import { Link } from "react-router-dom";
import "./Forum.css";

function Forum() {
  const [discussions, setDiscussions] = useState<ForumDiscussion[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isCurrentRequest = true;

    setLoading(true);
    setError("");

    getForumDiscussions(page)
      .then((result) => {
        if (!isCurrentRequest) return;

        setDiscussions(result.content);
        setTotalPages(result.totalPages);
      })
      .catch((requestError: unknown) => {
        if (!isCurrentRequest) return;

        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load forum discussions.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [page]);

  return (
    <main className="forum-page">
      <header className="forum-header">
        <div>
            <h1>Forum</h1>
            <p>Discuss the games you play.</p>
        </div>

        <Link className="forum-create-link" to="/forum/new">
            Start a discussion
        </Link>
        </header>

      {loading && <p role="status">Loading discussions...</p>}

      {!loading && error && (
        <p className="forum-error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && discussions.length === 0 && (
        <p className="forum-empty">There are no discussions yet.</p>
      )}

      {!loading && !error && discussions.length > 0 && (
        <>
          <section className="forum-list" aria-label="Forum discussions">
            {discussions.map((discussion) => (
              <Link
                className="forum-card"
                key={discussion.id}
                to={`/forum/${discussion.id}`}
                >

                <div className="forum-card__body">
                  <div className="forum-card__author">
                    {discussion.authorAvatarUrl ? (
                      <img
                        className="forum-avatar"
                        src={discussion.authorAvatarUrl}
                        alt=""
                        loading="lazy"
                      />
                    ) : (
                      <span className="forum-avatar forum-avatar--fallback">
                        {discussion.authorUsername.charAt(0).toUpperCase()}
                      </span>
                    )}

                    <span>{discussion.authorUsername}</span>
                    <time dateTime={discussion.createdAt}>
                      {new Date(discussion.createdAt).toLocaleDateString()}
                    </time>
                  </div>

                  <h2>{discussion.title}</h2>
                  <p className="forum-card__description">
                    {discussion.description}
                  </p>

                  <p className="forum-card__game">
                    Game: <strong>{discussion.gameTitle}</strong>
                  </p>

                  <ul className="forum-tags" aria-label="Discussion tags">
                    {discussion.tags.map((tag) => (
                      <li key={tag.id}>{tag.name}</li>
                    ))}
                  </ul>

                  <p className="forum-card__rating">
                    {discussion.averageRating === null
                      ? "No ratings yet"
                      : `${discussion.averageRating.toFixed(1)} / 5`}
                    {" · "}
                    {discussion.ratingCount}{" "}
                    {discussion.ratingCount === 1 ? "rating" : "ratings"}
                  </p>
                </div>
              </Link>
            ))}
          </section>

          {totalPages > 1 && (
            <nav className="forum-pagination" aria-label="Forum pages">
              <button
                type="button"
                onClick={() => setPage((current) => current - 1)}
                disabled={page === 0 || loading}
              >
                Previous
              </button>

              <span>
                Page {page + 1} of {totalPages}
              </span>

              <button
                type="button"
                onClick={() => setPage((current) => current + 1)}
                disabled={page + 1 >= totalPages || loading}
              >
                Next
              </button>
            </nav>
          )}
        </>
      )}
    </main>
  );
}

export default Forum;