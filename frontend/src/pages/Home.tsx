import GameGrid from "../components/games/GameGrid";
import { games } from "../data/games";
import "./Home.css";

function Home() {
    return (
        <main className="home">
            <h1>Ludarium</h1>

            <GameGrid games={games} />
        </main>
    );
}

export default Home;