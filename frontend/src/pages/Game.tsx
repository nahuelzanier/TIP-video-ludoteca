import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import GameSection from "../components/games/GameSection";
import GameTagSection from "../components/games/GameTagSection";
import CommentSection from "../components/comments/CommentSection";
import type { Game as GameType } from "../types/Game";
import { getGameById } from "../services/gameService";
import { API_BASE_URL } from "../services/api";
import "./Game.css";

function Game() {
  const { id } = useParams<{ id: string }>();

  const [game, setGame] = useState<GameType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let isCurrentRequest = true;

    if (!id) {
      setGame(null);
      setError("Game not found.");
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    setGame(null);

    getGameById(id)
      .then((loadedGame) => {
        if (isCurrentRequest) setGame(loadedGame);
      })
      .catch((requestError) => {
        if (!isCurrentRequest) return;

        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load the game.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [id]);

  if (loading) {
    return (
      <main className="game-page">
        <h1>Loading game...</h1>
      </main>
    );
  }

  if (error || !game) {
    return (
      <main className="game-page">
        <h1>{error || "Game not found."}</h1>
      </main>
    );
  }

  const gameUrl = `${API_BASE_URL}/games/${encodeURIComponent(game.id)}/index.html`;

  return (
    <main className="game-page">
      <header className="game-header">
        <h1>{game.title}</h1>
        <p>{game.description}</p>
      </header>

      <GameSection gameUrl={gameUrl} />
      <GameTagSection gameId={game.id} />
      <CommentSection gameId={game.id} />
    </main>
  );
}

export default Game;