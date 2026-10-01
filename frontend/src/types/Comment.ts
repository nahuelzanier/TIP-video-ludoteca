export interface Comment {
  id: number;
  content: string;
  rating: number | null;
  createdAt: string;
  username: string;
  profileImageUrl: string | null;
  isGameAuthor: boolean;
  replies: Comment[];
}

export interface CreateCommentPayload {
  content: string;
  rating?: number;
}