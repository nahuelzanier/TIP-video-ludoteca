export interface NewsItem {
  id: number;
  title: string;
  description: string;
  publishedAt: string;
  authorUsername: string;
  authorAvatarUrl: string | null;
  imageUrl: string | null;
}

export interface NewsPage {
  content: NewsItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface NewsPayload {
  title: string;
  description: string;
  image?: File | null;
}

export interface FollowStatus {
  username: string;
  following: boolean;
}

export interface FollowCounts {
  username: string;
  followersCount: number;
  followingCount: number;
}
