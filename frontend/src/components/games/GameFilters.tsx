import { useEffect, useState } from "react";
import { getAvailableTags, type TagOption } from "../../services/tagService";
import type { GameFilters as GameFiltersState } from "../../types/GameFilters";
import "./GameFilters.css";

interface GameFiltersProps {
  filters: GameFiltersState;
  onChange: (filters: GameFiltersState) => void;
}

function GameFilters({ filters, onChange }: GameFiltersProps) {
  const [tags, setTags] = useState<TagOption[]>([]);
  const [loadingTags, setLoadingTags] = useState(true);
  const [tagError, setTagError] = useState("");

  useEffect(() => {
    let isCurrentRequest = true;

    getAvailableTags()
      .then((result) => {
        if (isCurrentRequest) setTags(result);
      })
      .catch((requestError) => {
        if (!isCurrentRequest) return;
        setTagError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load genres.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoadingTags(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, []);

  function toggleTag(tagId: number, checked: boolean) {
    const tagIds = checked
      ? [...filters.tagIds, tagId]
      : filters.tagIds.filter((selectedId) => selectedId !== tagId);

    onChange({ ...filters, tagIds });
  }

  const hasActiveFilters = filters.tagIds.length > 0 || filters.minRating !== null;

  return (
    <section className="game-filters" aria-labelledby="game-filters-title">
      <div className="game-filters__header">
        <h2 id="game-filters-title">Filter games</h2>
        <button
          className="game-filters__clear"
          type="button"
          onClick={() => onChange({ tagIds: [], minRating: null })}
          disabled={!hasActiveFilters}
        >
          Clear filters
        </button>
      </div>

      <div className="game-filters__controls">
        <label className="game-filters__rating">
          <span>Minimum rating</span>
          <select
            value={filters.minRating === null ? "" : String(filters.minRating)}
            onChange={(event) => {
              const value = event.currentTarget.value;
              onChange({
                ...filters,
                minRating: value === "" ? null : Number(value),
              });
            }}
          >
            <option value="">Any rating</option>
            {[5, 4, 3, 2, 1].map((rating) => (
              <option key={rating} value={rating}>
                {rating}+ stars
              </option>
            ))}
          </select>
        </label>

        <fieldset className="game-filters__genres">
          <legend>Genres (all selected)</legend>
          {loadingTags && <p className="game-filters__message">Loading genres...</p>}
          {tagError && <p className="game-filters__error" role="alert">{tagError}</p>}
          {!loadingTags && !tagError && tags.length === 0 && (
            <p className="game-filters__message">No genres available yet.</p>
          )}
          {!tagError && tags.length > 0 && (
            <div className="game-filters__tag-list">
              {tags.map((tag) => (
                <label className="game-filters__tag" key={tag.id}>
                  <input
                    type="checkbox"
                    checked={filters.tagIds.includes(tag.id)}
                    onChange={(event) => toggleTag(tag.id, event.currentTarget.checked)}
                  />
                  <span>{tag.name}</span>
                </label>
              ))}
            </div>
          )}
        </fieldset>
      </div>
    </section>
  );
}

export default GameFilters;
