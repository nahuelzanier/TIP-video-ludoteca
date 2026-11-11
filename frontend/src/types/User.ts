export interface UserProfile {
    id: number;
    username: string;
    description: string | null;
    avatarUrl: string | null;
}

export interface ForumSummary {
    id: number;
    title: string;
    createdAt: string;
    coverImageUrl: string | null;
}
