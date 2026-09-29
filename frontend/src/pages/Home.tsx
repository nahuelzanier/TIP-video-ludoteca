import { useEffect, useState } from "react";
import GameGrid from "../components/games/GameGrid";
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

  useEffect(() => {
    let isCurrentRequest = true;

    setLoading(true);
    setError("");

    getGamesPage(page)
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
  }, [page]);

  return (
    <main className="home">
      <h1>Games</h1>

      <p className="game-count">
        {totalElements} {totalElements === 1 ? "game" : "games"}
      </p>

      {loading && <p>Loading games...</p>}
      {error && <p role="alert">{error}</p>}

      {!loading && !error && games.length === 0 && (
        <p>No published games yet.</p>
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