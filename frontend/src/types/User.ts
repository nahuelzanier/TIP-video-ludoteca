export interface UserProfile {
    id: number;
    username: string;
    description: string | null;
}

export interface ForumSummary {
    id: string;
    title: string;
    createdAt: string;
}
