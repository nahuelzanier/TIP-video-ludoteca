import type { Comment } from "../../types/Comment";
import CommentItem from "./CommentItem";

interface ReplyListProps {
  replies: Comment[];
  submitting: boolean;
  submitError: string;
  canReply: boolean;
  canEdit: boolean;
  onReply: (commentId: number, content: string) => Promise<boolean>;
  onEdit: (commentId: number, content: string) => Promise<boolean>;
}

function ReplyList({
  replies,
  submitting,
  submitError,
  canReply,
  canEdit,
  onReply,
  onEdit,
}: ReplyListProps) {
  if (replies.length === 0) {
    return null;
  }

  return (
    <div className="reply-list">
      {replies.map((reply) => (
        <CommentItem
          key={reply.id}
          comment={reply}
          variant="reply"
          submitting={submitting}
          submitError={submitError}
          canReply={canReply}
          canEdit={canEdit}
          onReply={onReply}
          onEdit={onEdit}
        />
      ))}
    </div>
  );
}

export default ReplyList;