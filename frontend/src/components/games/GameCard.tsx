import { Link } from "react-router-dom";
import type { Game } from "../../types/Game";
import "./GameCard.css";

interface GameCardProps {
    game: Game;
}

function GameCard({ game }: GameCardProps) {
    return (
        <Link className="game-card" to={`/game/${game.id}`}>
            <img src={game.image} alt={game.title} />

            <div className="game-card-content">
                <h2>{game.title}</h2>
                <p>{game.description}</p>
            </div>
        </Link>
    );
}

export default GameCard;