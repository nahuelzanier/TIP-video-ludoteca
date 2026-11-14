export interface Game {
    id: string;
    title: string;
    image: string;
    description: string;
    favorite?: boolean;
    uploaderUsername?: string;
    customization?: GameCustomization;
}

export interface GameCustomization {
    backgroundColor: string;
    accentColor: string;
    textColor: string;
    fontFamily: "SYSTEM" | "SERIF" | "MONO" | "ARIAL" | "VERDANA" | "TREBUCHET" | "GEORGIA" | "COURIER";
    backgroundStyle: "SOLID" | "GRADIENT";
}

export interface GameRecommendation {
    id: string;
    title: string;
    image: string;
}
