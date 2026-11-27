import { useCallback, useEffect, useState } from "react";
import {
  createComment,
  createNewsComment,
  createReply,
  getGameComments,
  getNewsComments,
  updateComment,
} from "../services/commentService";
import { getCurrentUser, type AuthUser } from "../services/authService";
import type { Comment } from "../types/Comment";

const LOAD_ERROR = "Could not load comments.";

export type CommentTarget = {
  type: "game" | "news";
  id: string;
};

type CommentTargetInput = string | CommentTarget;

interface UseCommentsResult {
  comments: Comment[];
  currentUser: AuthUser | null;
  loading: boolean;
  error: string;
  submitting: boolean;
  submitError: string;
  submitComment: (content: string, rating: number | null) => Promise<boolean>;
  submitReply: (commentId: number, content: string) => Promise<boolean>;
  submitEdit: (commentId: number, content: string) => Promise<boolean>;
}

function resolveTarget(input: CommentTargetInput): CommentTarget {
  return typeof input === "string" ? { type: "game", id: input } : input;
}

/**
 * A plain string keeps the original game-only contract; passing
 * `{ type: "news", id }` reuses the same state machine for news comments.
 */
export function useComments(input: CommentTargetInput): UseCommentsResult {
  const target = resolveTarget(input);
  const targetType = target.type;
  const targetId = target.id;

  const [comments, setComments] = useState<Comment[]>([]);
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const loadComments = useCallback(async (): Promise<Comment[]> => {
    return targetType === "news"
      ? getNewsComments(Number(targetId))
      : getGameComments(targetId);
  }, [targetType, targetId]);

  useEffect(() => {
    let cancelled = false;

    getCurrentUser()
      .then((user) => {
        if (!cancelled) {
          setCurrentUser(user);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCurrentUser(null);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;

    loadComments()
      .then((loaded) => {
        if (!cancelled) {
          setComments(loaded);
        }
      })
      .catch((requestError: unknown) => {
        if (!cancelled) {
          setError(messageOf(requestError, LOAD_ERROR));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [loadComments]);

  const publish = useCallback(
    async (send: () => Promise<Comment>): Promise<boolean> => {
      setSubmitting(true);
      setSubmitError("");

      try {
        await send();
        setComments(await loadComments());
        return true;
      } catch (requestError) {
        setSubmitError(messageOf(requestError, "Could not post your comment."));
        return false;
      } finally {
        setSubmitting(false);
      }
    },
    [loadComments],
  );

  const submitComment = useCallback(
    (content: string, rating: number | null) =>
      targetType === "news"
        ? publish(() => createNewsComment(Number(targetId), { content }))
        : publish(() =>
            createComment(targetId, rating === null ? { content } : { content, rating }),
          ),
    [targetType, targetId, publish],
  );

  const submitReply = useCallback(
    (commentId: number, content: string) =>
      publish(() => createReply(commentId, content)),
    [publish],
  );

  const submitEdit = useCallback(
    (commentId: number, content: string) =>
      publish(() => updateComment(commentId, content)),
    [publish],
  );

  return {
    comments,
    currentUser,
    loading,
    error,
    submitting,
    submitError,
    submitComment,
    submitReply,
    submitEdit,
  };
}

function messageOf(requestError: unknown, fallback: string): string {
  return requestError instanceof Error ? requestError.message : fallback;
}
