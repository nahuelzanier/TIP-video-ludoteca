import { useEffect, useState } from "react";
import type { ForumDiscussion, ForumTag } from "../../services/forumService";
import { getForumDiscussions, getForumTags } from "../../services/forumService";
import { Link } from "react-router-dom";
import "./Forum.css";

function Forum() {
  const [discussions, setDiscussions] = useState<ForumDiscussion[]>([]);
  const [tags, setTags] = useState<ForumTag[]>([]);
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [tagsError, setTagsError] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    let isCurrentRequest = true;

    getForumTags()
      .then((availableTags) => {
        if (isCurrentRequest) setTags(availableTags);
      })
      .catch(() => {
        if (isCurrentRequest) setTagsError("Could not load forum tags.");
      });

    return () => {
      isCurrentRequest = false;
    };
  }, []);

  useEffect(() => {
    const timeout = window.setTimeout(() => {
      setDebouncedQuery(query.trim());
      setPage(0);
    }, 300);

    return () => window.clearTimeout(timeout);
  }, [query]);

  useEffect(() => {
    let isCurrentRequest = true;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- pagination starts a new request and must show its pending state.
    setLoading(true);
    setError("");

    getForumDiscussions(page, 12, {
      query: debouncedQuery,
      tagIds: selectedTagIds,
    })
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
  }, [page, debouncedQuery, selectedTagIds]);

  function toggleTag(tagId: number) {
    setSelectedTagIds((current) =>
      current.includes(tagId)
        ? current.filter((selectedId) => selectedId !== tagId)
        : [...current, tagId],
    );
    setPage(0);
  }

  function clearFilters() {
    setQuery("");
    setDebouncedQuery("");
    setSelectedTagIds([]);
    setPage(0);
  }

  const hasActiveFilters = Boolean(query.trim()) || selectedTagIds.length > 0;
  const activeFilterCount = Number(Boolean(query.trim())) + selectedTagIds.length;

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

      <section className="forum-search-filters" aria-labelledby="forum-search-filters-title">
        <div className="forum-search-filters__header">
          <div>
            <h2 id="forum-search-filters-title">Find a discussion</h2>
            {hasActiveFilters && (
              <p className="forum-search-filters__summary" aria-live="polite">
                {activeFilterCount} active {activeFilterCount === 1 ? "filter" : "filters"}
              </p>
            )}
          </div>
          <div className="forum-search-filters__actions">
            {hasActiveFilters && (
              <button className="forum-clear-filters" type="button" onClick={clearFilters}>
                Clear
              </button>
            )}
            <button
              className="forum-filter-toggle"
              type="button"
              aria-expanded={filtersOpen}
              aria-controls="forum-filter-controls"
              onClick={() => setFiltersOpen((open) => !open)}
            >
              {filtersOpen ? "Hide filters" : "Show filters"}
            </button>
          </div>
        </div>

        <div
          className="forum-search-filters__content"
          id="forum-filter-controls"
          hidden={!filtersOpen}
        >
          <label className="forum-search-label" htmlFor="forum-search">
            Search discussions
          </label>
          <input
            id="forum-search"
            className="forum-search-input"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Title, description, or game"
          />

          <fieldset className="forum-filter-tags">
            <legend>Filter by forum tags</legend>
            <p className="forum-filter-hint">
              Selecting multiple tags shows discussions that include all of them.
            </p>
            {tags.length === 0 && !tagsError && <span>Loading tags...</span>}
            {tagsError && <span className="forum-filter-error" role="alert">{tagsError}</span>}
            {tags.map((tag) => (
              <label className="forum-filter-tag" key={tag.id}>
                <input
                  type="checkbox"
                  checked={selectedTagIds.includes(tag.id)}
                  onChange={() => toggleTag(tag.id)}
                />
                <span>{tag.name}</span>
              </label>
            ))}
          </fieldset>
        </div>
      </section>

      {loading && <p role="status">Loading discussions...</p>}

      {!loading && error && (
        <p className="forum-error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && discussions.length === 0 && (
        <p className="forum-empty">
          {hasActiveFilters
            ? "No discussions match your search and filters."
            : "There are no discussions yet."}
        </p>
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
                    <div
                      className="forum-card__cover forum-card__cover-fallback"
                      hidden
                      aria-hidden="true"
                    >
                      No cover image
                    </div>
                  </>
                ) : (
                  <div
                    className="forum-card__cover forum-card__cover-fallback"
                    aria-hidden="true"
                  >
                    No cover image
                  </div>
                )}
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
                      {new Date(discussion.createdAt).toLocaleDateString("en-US")}
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
