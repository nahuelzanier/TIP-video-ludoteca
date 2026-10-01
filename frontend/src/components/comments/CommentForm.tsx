import { useEffect, useRef, useState, type FormEvent } from "react";
import StarRating from "./StarRating";
import "./CommentForm.css";

const MAX_CONTENT_LENGTH = 1000;

interface CommentFormProps {
  placeholder: string;
  submitLabel: string;
  submitting: boolean;
  error: string;
  allowRating?: boolean;
  autoFocus?: boolean;
  onSubmit: (content: string, rating: number | null) => Promise<boolean>;
  onCancel?: () => void;
}

function CommentForm({
  placeholder,
  submitLabel,
  submitting,
  error,
  allowRating = false,
  autoFocus = false,
  onSubmit,
  onCancel,
}: CommentFormProps) {
  const [content, setContent] = useState("");
  const [rating, setRating] = useState(0);
  const [expanded, setExpanded] = useState(autoFocus);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (autoFocus) {
      textareaRef.current?.focus();
    }
  }, [autoFocus]);

  const remaining = MAX_CONTENT_LENGTH - content.length;
  const isTooLong = remaining < 0;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (submitting) {
      return;
    }

    void handlePublish();
  }

  async function handlePublish() {
    const published = await onSubmit(content.trim(), rating === 0 ? null : rating);

    if (published) {
      setContent("");
      setRating(0);
      setExpanded(false);
    }
  }

  function handleCancel() {
    setContent("");
    setRating(0);
    setExpanded(false);
    onCancel?.();
  }

  return (
    <form
      className={`comment-form${expanded ? " comment-form--expanded" : ""}`}
      onSubmit={handleSubmit}
    >
      <textarea
        ref={textareaRef}
        className="comment-form__input"
        placeholder={placeholder}
        value={content}
        rows={expanded ? 3 : 1}
        maxLength={MAX_CONTENT_LENGTH}
        disabled={submitting}
        onFocus={() => setExpanded(true)}
        onChange={(event) => setContent(event.target.value)}
      />

      {expanded && (
        <div className="comment-form__footer">
          {allowRating && (
            <StarRating value={rating} onChange={setRating} label="Tu valoración" />
          )}

          <div className="comment-form__actions">
            <span
              className={`comment-form__counter${isTooLong ? " comment-form__counter--error" : ""}`}
            >
              {remaining}
            </span>

            <button
              type="button"
              className="comment-form__cancel"
              onClick={handleCancel}
              disabled={submitting}
            >
              Cancelar
            </button>

            <button
              type="submit"
              className="comment-form__submit"
              disabled={submitting || content.trim().length === 0 || isTooLong}
            >
              {submitting ? "Subiendo..." : submitLabel}
            </button>
          </div>
        </div>
      )}

      {expanded && error && (
        <p className="comment-form__error" role="alert">
          {error}
        </p>
      )}
    </form>
  );
}

export default CommentForm;