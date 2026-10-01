import { useState } from "react";
import type { Comment } from "../../types/Comment";
import CommentForm from "./CommentForm";
import ReplyList from "./ReplyList";
import UserBadge from "./UserBadge";

interface CommentItemProps {
  comment: Comment;
  variant: "root" | "reply";
  submitting: boolean;
  submitError: string;
  canReply: boolean;
  onReply: (commentId: number, content: string) => Promise<boolean>;
}

function CommentItem({
  comment,
  variant,
  submitting,
  submitError,
  canReply,
  onReply,
}: CommentItemProps) {
  const [isReplying, setIsReplying] = useState(false);
  const itemClass =
    variant === "reply" ? "comment-item comment-item--reply" : "comment-item";

  async function handleReply(content: string): Promise<boolean> {
    const published = await onReply(comment.id, content);

    if (published) {
      setIsReplying(false);
    }

    return published;
  }

  return (
    <article className={itemClass}>
      <UserBadge
        username={comment.username}
        profileImageUrl={comment.profileImageUrl}
        createdAt={comment.createdAt}
        isGameAuthor={comment.isGameAuthor}
        rating={comment.rating}
        avatarSize={variant === "reply" ? "small" : "regular"}
      />

      <p className="comment-item__content">{comment.content}</p>

      {canReply && !isReplying && (
        <button
          type="button"
          className="comment-item__reply-button"
          onClick={() => setIsReplying(true)}
          disabled={submitting}
        >
          Responder
        </button>
      )}

      {isReplying && (
        <CommentForm
          placeholder={`Responder a ${comment.username}...`}
          submitLabel="Responder"
          submitting={submitting}
          error={submitError}
          autoFocus
          onSubmit={(content) => handleReply(content)}
          onCancel={() => setIsReplying(false)}
        />
      )}

      <ReplyList
        replies={comment.replies}
        submitting={submitting}
        submitError={submitError}
        canReply={canReply}
        onReply={onReply}
      />
    </article>
  );
}

export default CommentItem;