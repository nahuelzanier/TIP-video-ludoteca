import { useParams } from "react-router-dom";
import GameSection from "../components/games/GameSection";
import { games } from "../data/games";
import "./Game.css";

function Game() {
    const { id } = useParams<{ id: string }>();

    const game = games.find((game) => game.id === id);

    if (!game) {
        return (
            <main className="game-page">
                <h1>Juego no encontrado</h1>
            </main>
        );
    }

    return (
        <main className="game-page">
            <header className="game-header">
                <h1>{game.title}</h1>
                <p>{game.description}</p>
            </header>

            <GameSection gameUrl={game.link} />
        </main>
    );
}

export default Game;