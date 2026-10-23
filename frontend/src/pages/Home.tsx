import { useEffect, useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import GameGrid from "../components/games/GameGrid";
import GameFilters from "../components/games/GameFilters";
import type { GameFilters as GameFiltersState } from "../types/GameFilters";
import type { Game } from "../types/Game";
import { getGamesPage } from "../services/gameService";
import "./Home.css";

function Home() {
  const [games, setGames] = useState<Game[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [filters, setFilters] = useState<GameFiltersState>({
    tagIds: [],
    minRating: null,
  });
  const navigate = useNavigate();

  useEffect(() => {
    let isCurrentRequest = true;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- pagination and filters start a new request and must show its pending state.
    setLoading(true);
    setError("");

    getGamesPage(page, 12, "", filters)
      .then((result) => {
        if (!isCurrentRequest) return;

        setGames(result.content);
        setTotalPages(result.totalPages);
        setTotalElements(result.totalElements);
      })
      .catch((requestError) => {
        if (!isCurrentRequest) return;

        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load games.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [page, filters]);

  const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    setSearchQuery(event.target.value);
  };

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const term = searchQuery.trim();

    if (term.length < 2) {
      return;
    }

    const params = new URLSearchParams({ q: term });
    filters.tagIds.forEach((tagId) => params.append("tagIds", String(tagId)));
    if (filters.minRating !== null) {
      params.set("minRating", String(filters.minRating));
    }

    navigate(`/search?${params.toString()}`);
  };

  function updateFilters(nextFilters: GameFiltersState) {
    setPage(0);
    setFilters(nextFilters);
  }

  const hasActiveFilters = filters.tagIds.length > 0 || filters.minRating !== null;

  return (
    <main className="home">
      <h1>Games</h1>

      <form
        className="home-search-form"
        role="search"
        onSubmit={handleSearchSubmit}
      >
        <svg
          className="home-search-icon"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          aria-hidden="true"
        >
          <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
          <path
            d="M20 20l-3.6-3.6"
            stroke="currentColor"
            strokeWidth="2"
            strokeLinecap="round"
          />
        </svg>
        <input
          className="home-search"
          type="search"
          placeholder="Buscar juegos y usuarios"
          value={searchQuery}
          onChange={handleSearchChange}
          aria-label="Buscar juegos y usuarios"
          minLength={2}
          required
        />
      </form>

      <GameFilters filters={filters} onChange={updateFilters} />

      <p className="game-count">
        {totalElements} {totalElements === 1 ? "game" : "games"}
      </p>

      {loading && <p>Loading games...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && games.length === 0 && (
        <p>
          {hasActiveFilters
            ? "No games match the selected filters."
            : "No published games yet."}
        </p>
      )}

      {!loading && !error && games.length > 0 && (
        <>
          <GameGrid games={games} />

          {totalPages > 1 && (
            <nav className="game-pagination" aria-label="Game pages">
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

export default Home;
