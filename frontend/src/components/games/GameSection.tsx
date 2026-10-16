import "./GameSection.css";

interface GameSectionProps {
    gameUrl: string;
}

function GameSection({ gameUrl }: GameSectionProps) {
    return (
        <section className="game-section">
            <iframe
                src={gameUrl}
                title="Juego"
                className="game-frame"
                sandbox="allow-scripts allow-same-origin"
                />
        </section>
    );
}

export default GameSection;