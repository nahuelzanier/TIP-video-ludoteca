import { useEffect, useState } from "react";
import { ArrowRight, Gamepad2, Sparkles } from "lucide-react";
import { Link, useNavigate } from "react-router-dom";
import FavoriteButton from "../components/games/FavoriteButton";
import type { Game } from "../types/Game";
import {
  getForumDiscussions,
  type ForumDiscussion,
} from "../services/forumService";
import { getGamesPage } from "../services/gameService";
import { getAvailableTags, type TagOption } from "../services/tagService";
import "./Home.css";

function relativeTime(dateValue: string): string {
  const timestamp = new Date(dateValue).getTime();

  if (!Number.isFinite(timestamp)) return "recently";

  const elapsedSeconds = Math.max(0, Math.floor((Date.now() - timestamp) / 1000));
  if (elapsedSeconds < 60) return "just now";
  if (elapsedSeconds < 3600) return `${Math.floor(elapsedSeconds / 60)} min ago`;
  if (elapsedSeconds < 86400) return `${Math.floor(elapsedSeconds / 3600)} hr ago`;
  return `${Math.floor(elapsedSeconds / 86400)} days ago`;
}

function Home() {
  const [games, setGames] = useState<Game[]>([]);
  const [tags, setTags] = useState<TagOption[]>([]);
  const [discussions, setDiscussions] = useState<ForumDiscussion[]>([]);
  const [gameError, setGameError] = useState("");
  const [tagError, setTagError] = useState("");
  const [discussionError, setDiscussionError] = useState("");
  const [gamesLoading, setGamesLoading] = useState(true);
  const [tagsLoading, setTagsLoading] = useState(true);
  const [discussionsLoading, setDiscussionsLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    let isCurrentRequest = true;

    getGamesPage(0, 5)
      .then((result) => {
        if (isCurrentRequest) setGames(result.content);
      })
      .catch((requestError: unknown) => {
        if (isCurrentRequest) {
          setGameError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load games.",
          );
        }
      })
      .finally(() => {
        if (isCurrentRequest) setGamesLoading(false);
      });

    getAvailableTags()
      .then((result) => {
        if (isCurrentRequest) setTags(result);
      })
      .catch((requestError: unknown) => {
        if (isCurrentRequest) {
          setTagError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load categories.",
          );
        }
      })
      .finally(() => {
        if (isCurrentRequest) setTagsLoading(false);
      });

    getForumDiscussions(0, 3)
      .then((result) => {
        if (isCurrentRequest) setDiscussions(result.content);
      })
      .catch((requestError: unknown) => {
        if (isCurrentRequest) {
          setDiscussionError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load community activity.",
          );
        }
      })
      .finally(() => {
        if (isCurrentRequest) setDiscussionsLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, []);

  function handleCategoryClick(tag: TagOption) {
    const params = new URLSearchParams({ tagIds: String(tag.id) });
    navigate(`/search?${params.toString()}`);
  }

  function handleExplore() {
    navigate("/search");
  }

  function handleUpload() {
    navigate("/games/upload");
  }

  return (
    <main className="home">
      <div className="home__content">
        <section className="home__hero" aria-labelledby="home-title">
          <div className="home__hero-copy">
            <h1 id="home-title">
              Your place
              <br />
              to play
            </h1>
            <p className="home__intro">
              Discover incredible indie games, support their creators, and
              become part of the community.
            </p>
            <div className="home__hero-actions">
              <button className="home__button home__button--primary" onClick={handleExplore}>
                Explore games
              </button>
              <button className="home__button home__button--outline" onClick={handleUpload}>
                Upload your game
              </button>
            </div>
          </div>
          <div className="home__hero-art">
            <img src="/images/home-hero.png" alt="A traveler looks out over a fantasy valley at sunset" />
          </div>
        </section>

        <section className="home__featured" aria-labelledby="featured-title">
          <div className="home__section-heading">
            <h2 id="featured-title">Featured games</h2>
          </div>

          {gamesLoading && <p className="home__message">Loading games...</p>}
          {gameError && <p className="home__message home__message--error" role="alert">{gameError}</p>}
          {!gamesLoading && !gameError && games.length === 0 && (
            <p className="home__message">No games have been published yet. Be the first to share one!</p>
          )}
          {!gameError && games.length > 0 && (
            <div className="home__game-grid">
              {games.map((game) => (
                <article className="home-game-card" key={game.id}>
                  <Link className="home-game-card__cover" to={`/game/${encodeURIComponent(game.id)}`}>
                    <img src={game.image} alt={game.title} />
                  </Link>
                  <FavoriteButton
                    gameId={game.id}
                    initialFavorite={Boolean(game.favorite)}
                    className="home-game-card__favorite"
                  />
                  <Link className="home-game-card__body" to={`/game/${encodeURIComponent(game.id)}`}>
                    <h3>{game.title}</h3>
                    <p>{game.description}</p>
                  </Link>
                </article>
              ))}
            </div>
          )}
        </section>

        <div className="home__community-grid">
          <section className="home__categories" aria-labelledby="categories-title">
            <div className="home__section-heading">
              <h2 id="categories-title">Browse by category</h2>
              <Sparkles size={16} aria-hidden="true" />
            </div>
            {tagsLoading && <p className="home__message">Loading categories...</p>}
            {tagError && <p className="home__message home__message--error" role="alert">{tagError}</p>}
            {!tagsLoading && !tagError && tags.length === 0 && (
              <p className="home__message">No categories are available yet.</p>
            )}
            {!tagError && tags.length > 0 && (
              <div className="home__category-grid">
                {tags.slice(0, 8).map((tag) => (
                  <button
                    className="home__category"
                    key={tag.id}
                    type="button"
                    onClick={() => handleCategoryClick(tag)}
                  >
                    <Gamepad2 size={17} aria-hidden="true" />
                    <span>{tag.name}</span>
                  </button>
                ))}
              </div>
            )}
          </section>

          <section className="home__activity" aria-labelledby="activity-title">
            <div className="home__section-heading">
              <h2 id="activity-title">Recent activity</h2>
              <Link className="home__text-link" to="/forum">
                Community <ArrowRight size={15} aria-hidden="true" />
              </Link>
            </div>
            {discussionsLoading && <p className="home__message">Loading activity...</p>}
            {discussionError && <p className="home__message home__message--error" role="alert">{discussionError}</p>}
            {!discussionsLoading && !discussionError && discussions.length === 0 && (
              <p className="home__message">No discussions yet. Start one in the community!</p>
            )}
            {!discussionError && discussions.length > 0 && (
              <div className="home__activity-list">
                {discussions.map((discussion) => (
                  <Link
                    className="home__activity-item"
                    key={discussion.id}
                    to={`/forum/${discussion.id}`}
                  >
                    {discussion.coverImageUrl ? (
                      <img src={discussion.coverImageUrl} alt="" />
                    ) : (
                      <span className="home__activity-placeholder" aria-hidden="true">
                        <Gamepad2 size={19} />
                      </span>
                    )}
                    <span className="home__activity-copy">
                      <span className="home__activity-title">{discussion.title}</span>
                      <span className="home__activity-meta">
                        {discussion.authorUsername} · {relativeTime(discussion.createdAt)}
                      </span>
                    </span>
                    <span className="home__activity-tag">{discussion.gameTitle}</span>
                  </Link>
                ))}
              </div>
            )}
          </section>
        </div>
      </div>

    </main>
  );
}

export default Home;
