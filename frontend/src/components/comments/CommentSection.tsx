import { Link } from "react-router-dom";
import { useComments } from "../../hooks/useComments";
import { useTranslation } from "../../services/i18n";
import CommentForm from "./CommentForm";
import CommentItem from "./CommentItem";
import "./CommentSection.css";

interface CommentSectionProps {
  gameId?: string;
  newsId?: number;
}

function CommentSection({ gameId, newsId }: CommentSectionProps) {
  const { t } = useTranslation();
  const isNews = newsId !== undefined;
  const targetType = isNews ? "news" : "game";
  const targetId = isNews ? String(newsId) : (gameId ?? "");

  const {
    comments,
    currentUser,
    loading,
    error,
    submitting,
    submitError,
    submitComment,
    submitReply,
    submitEdit,
  } = useComments({ type: targetType, id: targetId });

  const isAuthenticated = currentUser !== null;

  return (
    <section className="comment-section" aria-labelledby="comment-section-title">
      <h2 id="comment-section-title" className="comment-section__title">
        {t("Comments")}
      </h2>

      {isAuthenticated ? (
        <CommentForm
          placeholder={
            isNews
              ? t("Share your thoughts about this news...")
              : t("Share your thoughts about this game...")
          }
          submitLabel={t("Post comment")}
          submitting={submitting}
          error={submitError}
          allowRating={!isNews}
          onSubmit={(content, rating) => submitComment(content, rating)}
        />
      ) : (
        <p className="comment-section__login-notice">
          <Link to="/login">{t("Log in")}</Link>{" "}
          {isNews
            ? t("to comment on this news.")
            : t("to comment on and rate this game.")}
        </p>
      )}

      {loading && (
        <p className="comment-section__status" role="status">
          {t("Loading comments...")}
        </p>
      )}

      {!loading && error && (
        <p className="comment-section__error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && comments.length === 0 && (
        <p className="comment-section__empty">
          {t("There are no comments yet. Be the first!")}
        </p>
      )}

      {!loading && !error && comments.length > 0 && (
        <div className="comment-section__list">
          {comments.map((comment) => (
            <CommentItem
              key={comment.id}
              comment={comment}
              variant="root"
              submitting={submitting}
              submitError={submitError}
              canReply={isAuthenticated}
              canEdit={currentUser?.username === comment.username}
              onReply={submitReply}
              onEdit={submitEdit}
            />
          ))}
        </div>
      )}
    </section>
  );
}

export default CommentSection;
