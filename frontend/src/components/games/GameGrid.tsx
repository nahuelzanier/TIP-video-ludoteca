import GameCard from "./GameCard";
import "./GameGrid.css";
import type { Game } from "../../types/Game";

interface GameGridProps {
    games: Game[];
}

function GameGrid({ games }: GameGridProps) {
    return (
        <div className="game-grid">
            {games.map((game) => (
                <GameCard key={game.id} game={game} />
            ))}
        </div>
    );
}

export default GameGrid;