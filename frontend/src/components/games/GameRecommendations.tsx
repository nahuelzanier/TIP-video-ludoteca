import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getGameRecommendations } from "../../services/gameService";
import type { GameRecommendation } from "../../types/Game";
import "./GameRecommendations.css";

interface GameRecommendationsProps {
    gameId: string;
}

function GameRecommendations({ gameId }: GameRecommendationsProps) {
    const [recommendations, setRecommendations] = useState<
        GameRecommendation[]
    >([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    const [renderedGameId, setRenderedGameId] = useState(gameId);
    if (renderedGameId !== gameId) {
        setRenderedGameId(gameId);
        setRecommendations([]);
        setError("");
        setLoading(true);
    }

    useEffect(() => {
        let isCurrent = true;

        getGameRecommendations(gameId)
            .then((loaded) => {
                if (isCurrent) setRecommendations(loaded);
            })
            .catch((requestError) => {
                if (!isCurrent) return;

                setError(
                    requestError instanceof Error
                        ? requestError.message
                        : "Could not load recommendations.",
                );
            })
            .finally(() => {
                if (isCurrent) setLoading(false);
            });

        return () => {
            isCurrent = false;
        };
    }, [gameId]);

    if (loading) {
        return (
            <section className="game-recommendations">
                <h2>Recomendaciones</h2>
                <p>Loading recommendations...</p>
            </section>
        );
    }

    if (error) {
        return (
            <section className="game-recommendations">
                <h2>Recomendaciones</h2>
                <p className="game-recommendations-error" role="alert">
                    {error}
                </p>
            </section>
        );
    }

    return (
        <section className="game-recommendations">
            <h2>Recomendaciones</h2>

            {recommendations.length > 0 ? (
                <div className="game-recommendation-grid">
                    {recommendations.map((game) => (
                        <RecommendationCard key={game.id} game={game} />
                    ))}
                </div>
            ) : (
                <p>No hay recomendaciones.</p>
            )}
        </section>
    );
}

function RecommendationCard({ game }: { game: GameRecommendation }) {
    const [failedUrl, setFailedUrl] = useState<string | null>(null);

    const showImage = Boolean(game.image) && failedUrl !== game.image;

    return (
        <Link className="game-recommendation-card" to={`/game/${game.id}`}>
            <span className="game-recommendation-image" aria-hidden="true">
                {showImage ? (
                    <img
                        src={game.image}
                        alt=""
                        loading="lazy"
                        onError={() => setFailedUrl(game.image)}
                    />
                ) : (
                    game.title.charAt(0).toUpperCase()
                )}
            </span>

            <h3>{game.title}</h3>
        </Link>
    );
}

export default GameRecommendations;
