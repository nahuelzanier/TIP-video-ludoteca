export interface Game {
    id: string;
    title: string;
    image: string;
    description: string;
    link: string;
}

export const games: Game[] = [
        {
        id: "juego-1",
        title: "Juego 1",
        image: "/games/juego-1/cover.png",
        description: "Una pequeña aventura.",
        link: "/games/juego-1/index.html",
    },
    {
        id: "juego-2",
        title: "Juego 2",
        image: "/games/juego-2/cover.png",
        description: "Una experiencia diferente.",
        link: "/games/juego-2/index.html",
    },
    {
        id: "juego-3",
        title: "Juego 3",
        image: "/games/juego-3/cover.png",
        description: "Un nuevo desafío.",
        link: "/games/juego-3/index.html",
    },
];
