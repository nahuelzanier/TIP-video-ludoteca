import { Link } from "react-router-dom";
import { useComments } from "../../hooks/useComments";
import CommentForm from "./CommentForm";
import CommentItem from "./CommentItem";
import "./CommentSection.css";

interface CommentSectionProps {
  gameId: string;
}

function CommentSection({ gameId }: CommentSectionProps) {
  const {
    comments,
    currentUser,
    loading,
    error,
    submitting,
    submitError,
    submitComment,
    submitReply,
  } = useComments(gameId);

  const isAuthenticated = currentUser !== null;

  return (
    <section className="comment-section" aria-labelledby="comment-section-title">
      <h2 id="comment-section-title" className="comment-section__title">
        Comentarios
      </h2>

      {isAuthenticated ? (
        <CommentForm
          placeholder="Compartí tu opinión sobre el juego..."
          submitLabel="Subir"
          submitting={submitting}
          error={submitError}
          allowRating
          onSubmit={(content, rating) => submitComment(content, rating)}
        />
      ) : (
        <p className="comment-section__login-notice">
          <Link to="/login">Iniciá sesión</Link> para comentar y valorar este juego.
        </p>
      )}

      {loading && (
        <p className="comment-section__status" role="status">
          Cargando comentarios...
        </p>
      )}

      {!loading && error && (
        <p className="comment-section__error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && comments.length === 0 && (
        <p className="comment-section__empty">
          Todavía no hay comentarios. ¡Sé el primero!
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
              onReply={submitReply}
            />
          ))}
        </div>
      )}
    </section>
  );
}

export default CommentSection;