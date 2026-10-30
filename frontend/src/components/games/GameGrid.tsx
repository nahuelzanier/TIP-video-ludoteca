import GameCard from "./GameCard";
import "./GameGrid.css";
import type { Game } from "../../types/Game";

interface GameGridProps {
    games: Game[];
    onFavoriteChange?: (gameId: string, nextFavorite: boolean) => void;
}

function GameGrid({ games, onFavoriteChange }: GameGridProps) {
    return (
        <div className="game-grid">
            {games.map((game) => (
                <GameCard
                    key={game.id}
                    game={game}
                    onFavoriteChange={onFavoriteChange}
                />
            ))}
        </div>
    );
}

export default GameGrid;