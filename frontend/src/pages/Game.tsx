import { useEffect, useState, type CSSProperties } from "react";
import { Link, useParams } from "react-router-dom";
import GameSection from "../components/games/GameSection";
import GameTagSection from "../components/games/GameTagSection";
import GameRecommendations from "../components/games/GameRecommendations";
import CommentSection from "../components/comments/CommentSection";
import FavoriteButton from "../components/games/FavoriteButton";
import type { Game as GameType } from "../types/Game";
import { getGameById } from "../services/gameService";
import { useTranslation } from "../services/i18n";
import { getCurrentUser, type AuthUser } from "../services/authService";
import GameCustomizationEditor from "../components/games/GameCustomizationEditor";
import { GAME_CONTENT_BASE_URL } from "../services/api";
import "./Game.css";

function Game() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();

  const [game, setGame] = useState<GameType | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);

  useEffect(() => {
    getCurrentUser().then(setCurrentUser).catch(() => setCurrentUser(null));
  }, []);

  useEffect(() => {
    let isCurrentRequest = true;

    if (!id) {
      // Resolve a missing route ID immediately instead of starting a game request.
      // eslint-disable-next-line react-hooks/set-state-in-effect -- keeps the no-ID route state terminal.
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
        <h1>{t("Loading game...")}</h1>
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

  const gameUrl = `${GAME_CONTENT_BASE_URL}/games/${encodeURIComponent(game.id)}/index.html`;
  const customization = game.customization ?? {
    backgroundColor: "#17151f",
    accentColor: "#a36aff",
    textColor: "#f5f4f8",
    fontFamily: "SYSTEM" as const,
    backgroundStyle: "SOLID" as const,
  };
  const fontFamilies = {
    SYSTEM: "system-ui, 'Segoe UI', sans-serif",
    SERIF: "Georgia, 'Times New Roman', serif",
    MONO: "'Courier New', monospace",
    ARIAL: "Arial, Helvetica, sans-serif",
    VERDANA: "Verdana, Geneva, sans-serif",
    TREBUCHET: "'Trebuchet MS', sans-serif",
    GEORGIA: "Georgia, 'Times New Roman', serif",
    COURIER: "'Courier New', Courier, monospace",
  };
  const pageStyle = {
    "--game-page-background": customization.backgroundColor,
    "--game-page-text": customization.textColor,
    "--text": customization.textColor,
    "--text-h": customization.textColor,
    "--text-muted": `color-mix(in srgb, ${customization.textColor} 68%, ${customization.backgroundColor})`,
    "--heading": fontFamilies[customization.fontFamily],
    "--sans": fontFamilies[customization.fontFamily],
    "--accent": customization.accentColor,
    "--accent-hover": customization.accentColor,
    "--accent-border": customization.accentColor,
    "--accent-bg": `${customization.accentColor}22`,
    "--accent-bg-strong": `${customization.accentColor}44`,
    "--game-page-font": fontFamilies[customization.fontFamily],
  } as CSSProperties;
  const isOwner = currentUser?.username === game.uploaderUsername;

  return (
    <main className={`game-page${customization.backgroundStyle === "GRADIENT" ? " game-page--gradient" : ""}`} style={pageStyle}>
      <header className="game-header">
        <div className="game-header-main">
          <div>
            <h1>{game.title}</h1>
            <p>{game.description}</p>
            {game.uploaderUsername && (
              <p className="game-uploader">
                Uploaded by <Link to={`/user/${encodeURIComponent(game.uploaderUsername)}`}>{game.uploaderUsername}</Link>
              </p>
            )}
          </div>
          <FavoriteButton
            gameId={game.id}
            initialFavorite={Boolean(game.favorite)}
          />
        </div>
      </header>

      {isOwner && (
        <GameCustomizationEditor
          gameId={game.id}
          customization={customization}
          onChange={(next) => setGame((current) => current ? { ...current, customization: next } : current)}
        />
      )}

      <GameSection gameUrl={gameUrl} />
      <GameTagSection gameId={game.id} />
      <CommentSection gameId={game.id} />
      <GameRecommendations gameId={game.id} />
    </main>
  );
}

export default Game;
