export interface Game {
    id: string;
    title: string;
    image: string;
    description: string;
    favorite?: boolean;
}

export interface GameRecommendation {
    id: string;
    title: string;
    image: string;
}