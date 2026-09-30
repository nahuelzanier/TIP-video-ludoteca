import type { Game } from "./Game";

export interface SearchUser {
    id: number;
    username: string;
}

export interface PageResult<T> {
    items: T[];
    page: number;
    size: number;
    totalItems: number;
    totalPages: number;
}

export type GamePage = PageResult<Game>;

export type UserPage = PageResult<SearchUser>;

export interface SearchResponse {
    query: string;
    games: GamePage;
    users: UserPage;
}
