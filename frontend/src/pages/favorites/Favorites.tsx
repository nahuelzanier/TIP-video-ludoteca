import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import GameGrid from "../../components/games/GameGrid";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { getFavoriteGamesPage } from "../../services/favoriteService";
import type { Game } from "../../types/Game";
import "./Favorites.css";

function Favorites() {
  const location = useLocation();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [games, setGames] = useState<Game[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isCurrent = true;

    async function loadFavorites() {
      setLoading(true);
      setError("");

      try {
        const currentUser = await getCurrentUser();
        if (!isCurrent) return;

        if (!currentUser) {
          setUser(null);
          setGames([]);
          setTotalPages(0);
          return;
        }

        setUser(currentUser);
        const result = await getFavoriteGamesPage(page, 9);

        if (!isCurrent) return;

        setGames(result.content);
        setTotalPages(result.totalPages);
      } catch (requestError) {
        if (!isCurrent) return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "No se pudieron cargar tus favoritos.",
        );
      } finally {
        if (isCurrent) {
          setLoading(false);
        }
      }
    }

    loadFavorites();

    return () => {
      isCurrent = false;
    };
  }, [page]);

  const handleFavoriteChange = (gameId: string, nextFavorite: boolean) => {
    if (!nextFavorite) {
      setGames((currentGames) => {
        const nextGames = currentGames.filter((game) => game.id !== gameId);

        if (nextGames.length === 0 && page > 0) {
          setPage((currentPage) => Math.max(0, currentPage - 1));
        }

        return nextGames;
      });
    }
  };

  if (!loading && !user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return (
    <main className="favorites-page">
      <header className="favorites-header">
        <h1>Mis favoritos</h1>
      </header>

      {loading && <p className="favorites-status">Cargando favoritos...</p>}
      {!loading && error && <p className="favorites-status" role="alert">{error}</p>}

      {!loading && !error && games.length === 0 && (
        <p className="favorites-empty">Todavía no tenés favoritos.</p>
      )}

      {!loading && !error && games.length > 0 && (
        <>
          <GameGrid games={games} onFavoriteChange={handleFavoriteChange} />

          {totalPages > 1 && (
            <nav className="favorites-pagination" aria-label="Páginas de favoritos">
              <button
                type="button"
                onClick={() => setPage((current) => current - 1)}
                disabled={page === 0}
              >
                Anterior
              </button>

              <span>
                Página {page + 1} de {totalPages}
              </span>

              <button
                type="button"
                onClick={() => setPage((current) => current + 1)}
                disabled={page + 1 >= totalPages}
              >
                Siguiente
              </button>
            </nav>
          )}
        </>
      )}
    </main>
  );
}

export default Favorites;
