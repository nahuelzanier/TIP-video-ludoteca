import { useState, type FormEvent } from "react";
import { Pencil } from "lucide-react";
import type { Comment } from "../../types/Comment";
import CommentForm from "./CommentForm";
import ReplyList from "./ReplyList";
import UserBadge from "./UserBadge";

const MAX_CONTENT_LENGTH = 1000;

interface CommentItemProps {
  comment: Comment;
  variant: "root" | "reply";
  submitting: boolean;
  submitError: string;
  canReply: boolean;
  canEdit: boolean;
  onReply: (commentId: number, content: string) => Promise<boolean>;
  onEdit: (commentId: number, content: string) => Promise<boolean>;
}

function CommentItem({
  comment,
  variant,
  submitting,
  submitError,
  canReply,
  canEdit,
  onReply,
  onEdit,
}: CommentItemProps) {
  const [isReplying, setIsReplying] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [localEditError, setLocalEditError] = useState("");

  const itemClass =
    variant === "reply" ? "comment-item comment-item--reply" : "comment-item";

  const remainingEdit = MAX_CONTENT_LENGTH - editContent.length;
  const isTooLongEdit = remainingEdit < 0;

  async function handleReply(content: string): Promise<boolean> {
    const published = await onReply(comment.id, content);

    if (published) {
      setIsReplying(false);
    }

    return published;
  }

  async function handleEditSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submitting) {
      return;
    }

    const trimmed = editContent.trim();

    if (trimmed.length === 0) {
      setLocalEditError("El comentario no puede estar vacío.");
      return;
    }

    const updated = await onEdit(comment.id, trimmed);

    if (updated) {
      setIsEditing(false);
      setLocalEditError("");
    } else {
      setLocalEditError(submitError || "No se pudo guardar la edición.");
    }
  }

  function handleEditCancel() {
    setIsEditing(false);
    setEditContent(comment.content);
    setLocalEditError("");
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
        edited={comment.edited}
      />

      {isEditing ? (
        <form className="comment-item__edit" onSubmit={handleEditSubmit}>
          <textarea
            className="comment-item__edit-textarea"
            value={editContent}
            rows={3}
            maxLength={MAX_CONTENT_LENGTH}
            disabled={submitting}
            onChange={(event) => {
              setEditContent(event.target.value);
              if (localEditError) {
                setLocalEditError("");
              }
            }}
            autoFocus
          />

          <div className="comment-item__edit-footer">
            <span
              className={`comment-item__edit-counter${
                isTooLongEdit ? " comment-item__edit-counter--error" : ""
              }`}
            >
              {remainingEdit}
            </span>

            <div className="comment-item__edit-actions">
              <button
                type="button"
                className="comment-form__cancel"
                onClick={handleEditCancel}
                disabled={submitting}
              >
                Cancelar
              </button>

              <button
                type="submit"
                className="comment-form__submit"
                disabled={
                  submitting ||
                  editContent.trim().length === 0 ||
                  isTooLongEdit ||
                  editContent.trim() === comment.content.trim()
                }
              >
                {submitting ? "Guardando..." : "Guardar"}
              </button>
            </div>
          </div>

          {(localEditError || submitError) && (
            <p className="comment-item__edit-error" role="alert">
              {localEditError || submitError}
            </p>
          )}
        </form>
      ) : (
        <>
          <p className="comment-item__content">{comment.content}</p>

          <div className="comment-item__actions">
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

            {canEdit && !isReplying && (
              <button
                type="button"
                className="comment-item__edit-button"
                onClick={() => {
                  setIsEditing(true);
                  setEditContent(comment.content);
                }}
                disabled={submitting}
                aria-label="Editar comentario"
                title="Editar comentario"
              >
                <Pencil size={14} aria-hidden="true" /> Editar
              </button>
            )}
          </div>
        </>
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
        canEdit={canEdit}
        onReply={onReply}
        onEdit={onEdit}
      />
    </article>
  );
}

export default CommentItem;
