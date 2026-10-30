import { Link } from "react-router-dom";
import type { Game } from "../../types/Game";
import FavoriteButton from "./FavoriteButton";
import "./GameCard.css";

interface GameCardProps {
    game: Game;
    onFavoriteChange?: (gameId: string, nextFavorite: boolean) => void;
}

function GameCard({ game, onFavoriteChange }: GameCardProps) {
    return (
        <article className="game-card">
            <div className="game-card-top">
                <Link className="game-card-cover" to={`/game/${game.id}`}>
                    <img src={game.image} alt={game.title} />
                </Link>

                <FavoriteButton
                    gameId={game.id}
                    initialFavorite={Boolean(game.favorite)}
                    onFavoriteChange={onFavoriteChange}
                    className="game-card-favorite"
                />
            </div>

            <Link className="game-card-content" to={`/game/${game.id}`}>
                <h2>{game.title}</h2>
                <p>{game.description}</p>
            </Link>
        </article>
    );
}

export default GameCard;