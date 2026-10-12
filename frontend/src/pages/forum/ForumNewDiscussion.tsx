import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { getGamesPage } from "../../services/gameService";
import {
  createForumDiscussion,
} from "../../services/forumService";
import { getAvailableTags, type TagOption } from "../../services/tagService";
import type { Game } from "../../types/Game";
import "./ForumNewDiscussion.css";

const MAX_COVER_SIZE = 5 * 1024 * 1024;
const MAX_TAGS = 5;

function ForumNewDiscussion() {
  const location = useLocation();
  const navigate = useNavigate();

  const [user, setUser] = useState<AuthUser | null>(null);
  const [games, setGames] = useState<Game[]>([]);
  const [tags, setTags] = useState<TagOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  const [gameId, setGameId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [cover, setCover] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState("");
  const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);

    const [gameQuery, setGameQuery] = useState("");
    const [searchingGames, setSearchingGames] = useState(false);

  useEffect(() => {
    let isCurrentRequest = true;

    getCurrentUser()
      .then(async (currentUser) => {
        if (!isCurrentRequest) return;
        setUser(currentUser);

        if (!currentUser) return;

        const availableTags = await getAvailableTags();

        if (!isCurrentRequest) return;
        setTags(availableTags);
      })
      .catch((requestError: unknown) => {
        if (!isCurrentRequest) return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load the form.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, []);
useEffect(() => {
  const query = gameQuery.trim();

  if (query.length < 2 || gameId) {
    setGames([]);
    return;
  }

  let isCurrentRequest = true;

  const timeout = window.setTimeout(() => {
    setSearchingGames(true);

    getGamesPage(0, 8, query)
      .then((page) => {
        if (isCurrentRequest) setGames(page.content);
      })
      .catch(() => {
        if (isCurrentRequest) setGames([]);
      })
      .finally(() => {
        if (isCurrentRequest) setSearchingGames(false);
      });
  }, 300);

  return () => {
    isCurrentRequest = false;
    window.clearTimeout(timeout);
  };
}, [gameQuery, gameId]);
  useEffect(() => {
    if (!cover) {
      setCoverPreview("");
      return;
    }

    const previewUrl = URL.createObjectURL(cover);
    setCoverPreview(previewUrl);

    return () => URL.revokeObjectURL(previewUrl);
  }, [cover]);

  function handleCoverChange(file: File | null) {
    setError("");

    if (!file) {
      setCover(null);
      return;
    }

    if (!["image/png", "image/jpeg"].includes(file.type)) {
      setCover(null);
      setError("The cover must be a PNG or JPEG image.");
      return;
    }

    if (file.size > MAX_COVER_SIZE) {
      setCover(null);
      setError("The cover image cannot exceed 5 MB.");
      return;
    }

    setCover(file);
  }

  function toggleTag(tagId: number) {
    setError("");

    setSelectedTagIds((current) => {
      if (current.includes(tagId)) {
        return current.filter((id) => id !== tagId);
      }

      if (current.length >= MAX_TAGS) {
        setError(`Choose no more than ${MAX_TAGS} tags.`);
        return current;
      }

      return [...current, tagId];
    });
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");

    if (!gameId || selectedTagIds.length === 0) {
      setError("Choose a game, a cover image, and at least one tag.");
      return;
    }

    setSaving(true);

    try {
      const created = await createForumDiscussion({
        gameId,
        title: title.trim(),
        description: description.trim(),
        cover,
        tagIds: selectedTagIds,
      });

      navigate(`/forum/${created.id}`, { replace: true });
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not create the discussion.",
      );
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <main className="forum-new-page">
        <p role="status">Loading form...</p>
      </main>
    );
  }

  if (error && !user) {
    return (
      <main className="forum-new-page">
        <p className="forum-new-error" role="alert">{error}</p>
      </main>
    );
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  return (
    <main className="forum-new-page">
      <Link className="forum-new-back" to="/forum">
        ← Back to forum
      </Link>

      <section className="forum-new-card">
        <header>
          <p className="forum-new-eyebrow">Community</p>
          <h1>Start a discussion</h1>
          <p>Share your thoughts about a published game.</p>
        </header>

        <form className="forum-new-form" onSubmit={handleSubmit}>
            <label htmlFor="forum-game">Game</label>
            <label htmlFor="forum-game">Game</label>
<input
  id="forum-game"
  type="search"
  value={gameQuery}
  onChange={(event) => {
    setGameQuery(event.target.value);
    setGameId("");
  }}
  placeholder="Search published games..."
  autoComplete="off"
  required
/>

{searchingGames && <p role="status">Searching games...</p>}

{!gameId && games.length > 0 && (
  <ul className="forum-game-results">
    {games.map((game) => (
      <li key={game.id}>
        <button
          type="button"
          onClick={() => {
            setGameId(game.id);
            setGameQuery(game.title);
            setGames([]);
          }}
        >
          {game.title}
        </button>
      </li>
    ))}
  </ul>
)}

            <label htmlFor="forum-title">Title</label>
            <input
              id="forum-title"
              type="text"
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              maxLength={120}
              required
            />

            <label htmlFor="forum-description">Description</label>
            <textarea
              id="forum-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              maxLength={2000}
              rows={5}
              required
            />

            <label htmlFor="forum-cover">Discussion image</label>
            <p className="forum-new-help">
            Optional. PNG or JPEG, up to 5 MB.
            </p>
            <input
              id="forum-cover"
              type="file"
              accept="image/png,image/jpeg"
              onChange={(event) =>
                handleCoverChange(event.target.files?.[0] ?? null)
              }
            />

            {coverPreview && (
              <img
                className="forum-new-preview"
                src={coverPreview}
                alt="Discussion cover preview"
              />
            )}

            <fieldset className="forum-new-tags">
              <legend>Tags (choose 1 to 5)</legend>
              {tags.map((tag) => (
                <label key={tag.id} className="forum-new-tag">
                  <input
                    type="checkbox"
                    checked={selectedTagIds.includes(tag.id)}
                    onChange={() => toggleTag(tag.id)}
                  />
                  {tag.name}
                </label>
              ))}
            </fieldset>

            {error && (
              <p className="forum-new-error" role="alert">
                {error}
              </p>
            )}

            <button
              className="forum-new-submit"
              type="submit"
              disabled={saving || tags.length === 0}
            >
              {saving ? "Creating..." : "Create discussion"}
            </button>
          </form>
        
      </section>
    </main>
  );
}

export default ForumNewDiscussion;