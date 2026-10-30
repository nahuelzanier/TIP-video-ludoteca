import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  getCurrentUser,
  type AuthUser,
} from "../../services/authService";
import {
  assignGameTag,
  getAvailableTags,
  getGameTags,
  getMyGameTagIds,
  type GameTag,
  type TagOption,
} from "../../services/tagService";
import "./GameTagSection.css";

interface GameTagSectionProps {
  gameId: string;
}

function GameTagSection({ gameId }: GameTagSectionProps) {
  const [availableTags, setAvailableTags] = useState<TagOption[]>([]);
  const [gameTags, setGameTags] = useState<GameTag[]>([]);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [myTagIds, setMyTagIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [assigningTagId, setAssigningTagId] = useState<number | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    let isCurrent = true;

    async function loadTags() {
      try {
        const [allTags, assignedTags, currentUser] = await Promise.all([
          getAvailableTags(),
          getGameTags(gameId),
          getCurrentUser(),
        ]);

        let ownTagIds: number[] = [];
        if (currentUser) {
          ownTagIds = await getMyGameTagIds(gameId);
        }

        if (!isCurrent) return;

        setAvailableTags(allTags);
        setGameTags(assignedTags);
        setUser(currentUser);
        setMyTagIds(new Set(ownTagIds));
      } catch (requestError) {
        if (!isCurrent) return;

        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load game tags.",
        );
      } finally {
        if (isCurrent) setLoading(false);
      }
    }

    loadTags();

    return () => {
      isCurrent = false;
    };
  }, [gameId]);

  async function handleAssignTag(tag: TagOption) {
    setError("");
    setAssigningTagId(tag.id);

    try {
      const updatedTags = await assignGameTag(gameId, tag.id);
      setGameTags(updatedTags);
      setMyTagIds((current) => new Set(current).add(tag.id));
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not assign this tag.",
      );
    } finally {
      setAssigningTagId(null);
    }
  }

  if (loading) {
    return (
      <section className="game-tags">
        <h2>Community tags</h2>
        <p>Loading tags...</p>
      </section>
    );
  }

  return (
    <section className="game-tags">
      <h2>Community tags</h2>

      {gameTags.length > 0 ? (
        <ul className="game-tag-list">
          {gameTags.map((tag) => (
            <li key={tag.tagId}>
              <span>{tag.name}</span>
              <span className="game-tag-count">{tag.assignmentCount}</span>
            </li>
          ))}
        </ul>
      ) : (
        <p>No tags yet. Be the first to tag this game.</p>
      )}

      {user ? (
        <div className="game-tag-picker">
          <h3>Which tags fit this game?</h3>

          <div className="game-tag-options">
            {availableTags.map((tag) => {
              const alreadyAssigned = myTagIds.has(tag.id);

              return (
                <button
                  key={tag.id}
                  type="button"
                  onClick={() => handleAssignTag(tag)}
                  disabled={alreadyAssigned || assigningTagId !== null}
                  title={
                    alreadyAssigned
                      ? "You have already assigned this tag."
                      : `Assign the ${tag.name} tag`
                  }
                >
                  {tag.name}
                  {alreadyAssigned ? " ✓" : ""}
                </button>
              );
            })}
          </div>
        </div>
      ) : (
        <p>
          <Link
            to="/login"
            state={{ from: `/game/${gameId}` }}
          >
            Log in
          </Link>{" "}
          to assign tags.
        </p>
      )}

      {error && <p className="game-tags-error" role="alert">{error}</p>}
    </section>
  );
}

export default GameTagSection;