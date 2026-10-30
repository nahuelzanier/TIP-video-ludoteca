import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getCurrentUser } from "../../services/authService";
import { toggleFavorite } from "../../services/favoriteService";
import "./FavoriteButton.css";

interface FavoriteButtonProps {
  gameId: string;
  initialFavorite?: boolean;
  onFavoriteChange?: (gameId: string, nextFavorite: boolean) => void;
  className?: string;
}

function FavoriteButton({
  gameId,
  initialFavorite = false,
  onFavoriteChange,
  className = "",
}: FavoriteButtonProps) {
  const [favorite, setFavorite] = useState(initialFavorite);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    setFavorite(initialFavorite);
  }, [initialFavorite]);

  async function handleClick() {
    if (loading) {
      return;
    }

    const currentUser = await getCurrentUser();
    if (!currentUser) {
      navigate("/login", { state: { from: location.pathname } });
      return;
    }

    setLoading(true);
    setError("");

    try {
      const updatedFavorite = await toggleFavorite(gameId, favorite);
      setFavorite(updatedFavorite);
      onFavoriteChange?.(gameId, updatedFavorite);
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "No se pudo actualizar tu favorito.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      <button
        type="button"
        className={`favorite-button ${favorite ? "favorite-button--active" : ""} ${className}`.trim()}
        onClick={handleClick}
        disabled={loading}
        aria-label={favorite ? "Quitar de favoritos" : "Guardar en favoritos"}
        aria-pressed={favorite}
        title={favorite ? "Quitar de favoritos" : "Guardar en favoritos"}
      >
        <span aria-hidden="true">{favorite ? "♥" : "♡"}</span>
      </button>
      {error && <span className="favorite-button-error">{error}</span>}
    </>
  );
}

export default FavoriteButton;
