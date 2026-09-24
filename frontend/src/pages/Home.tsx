import { useEffect, useState } from "react";
import GameGrid from "../components/games/GameGrid";
import type { Game } from "../types/Game";
import { getGames } from "../services/gameService";
import "./Home.css";

function Home() {
    const [games, setGames] = useState<Game[]>([]);

    useEffect(() => {
        getGames().then((data) => {
            setGames(data);
        });
    }, []);

    return (
        <main className="home">
            <h1>Juegos</h1>

            <GameGrid games={games} />
        </main>
    );
}

export default Home;