import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import GameSection from "../components/games/GameSection";
import type { Game as GameType } from "../types/Game";
import { getGames } from "../services/gameService";
import "./Game.css";

function Game() {
    const { id } = useParams<{ id: string }>();
    const [game, setGame] = useState<GameType | null>(null);

    useEffect(() => {
        getGames().then((games) => {
            const foundGame = games.find((game) => game.id === id);
            setGame(foundGame ?? null);
        });
    }, [id]);

    if (!game) {
        return (
            <main className="game-page">
                <h1>Juego no encontrado</h1>
            </main>
        );
    }

    const gameUrl = `http://localhost:8080/games/${game.id}/index.html`;

    return (
        <main className="game-page">
            <header className="game-header">
                <h1>{game.title}</h1>
                <p>{game.description}</p>
            </header>

            <GameSection gameUrl={gameUrl} />
        </main>
    );
}

export default Game;