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
            />
        </section>
    );
}

export default GameSection;