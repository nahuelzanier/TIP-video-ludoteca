import { useCallback, useEffect, useState } from "react";
import {
  createComment,
  createReply,
  getGameComments,
} from "../services/commentService";
import { getCurrentUser, type AuthUser } from "../services/authService";
import type { Comment } from "../types/Comment";

const LOAD_ERROR = "No se pudieron cargar los comentarios.";

interface UseCommentsResult {
  comments: Comment[];
  currentUser: AuthUser | null;
  loading: boolean;
  error: string;
  submitting: boolean;
  submitError: string;
  submitComment: (content: string, rating: number | null) => Promise<boolean>;
  submitReply: (commentId: number, content: string) => Promise<boolean>;
}

export function useComments(gameId: string): UseCommentsResult {
  const [comments, setComments] = useState<Comment[]>([]);
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");

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

    getGameComments(gameId)
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
  }, [gameId]);

  const publish = useCallback(
    async (send: () => Promise<Comment>): Promise<boolean> => {
      setSubmitting(true);
      setSubmitError("");

      try {
        await send();
        setComments(await getGameComments(gameId));
        return true;
      } catch (requestError) {
        setSubmitError(messageOf(requestError, "No se pudo publicar."));
        return false;
      } finally {
        setSubmitting(false);
      }
    },
    [gameId],
  );

  const submitComment = useCallback(
    (content: string, rating: number | null) =>
      publish(() =>
        createComment(gameId, rating === null ? { content } : { content, rating }),
      ),
    [gameId, publish],
  );

  const submitReply = useCallback(
    (commentId: number, content: string) =>
      publish(() => createReply(commentId, content)),
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
  };
}

function messageOf(requestError: unknown, fallback: string): string {
  return requestError instanceof Error ? requestError.message : fallback;
}