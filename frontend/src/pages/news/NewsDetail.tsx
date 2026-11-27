import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Trash2 } from "lucide-react";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { deleteNews, getNews } from "../../services/newsService";
import { useTranslation } from "../../services/i18n";
import CommentSection from "../../components/comments/CommentSection";
import type { NewsItem } from "../../types/News";
import "./NewsDetail.css";

function NewsDetail() {
  const { t } = useTranslation();
  const { id: rawId } = useParams();
  const navigate = useNavigate();
  const noticiaId = Number(rawId);
  const validId = Number.isInteger(noticiaId) && noticiaId > 0;

  const [news, setNews] = useState<NewsItem | null>(null);
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");

  useEffect(() => {
    if (!validId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- invalid route IDs resolve without a request.
      setError("News not found.");
      setLoading(false);
      return;
    }

    let isCurrentRequest = true;

    Promise.all([
      getNews(noticiaId),
      getCurrentUser().catch(() => null),
    ])
      .then(([loadedNews, user]) => {
        if (!isCurrentRequest) return;
        setNews(loadedNews);
        setCurrentUser(user);
      })
      .catch((requestError: unknown) => {
        if (!isCurrentRequest) return;
        setError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load this news.",
        );
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [noticiaId, validId]);

  async function handleDelete() {
    if (!news || deleting) return;

    if (!window.confirm(t("Delete this news?"))) {
      return;
    }

    setDeleteError("");
    setDeleting(true);

    try {
      await deleteNews(news.id);
      navigate("/noticias", { replace: true });
    } catch (requestError) {
      setDeleteError(
        requestError instanceof Error
          ? requestError.message
          : "Could not delete this news.",
      );
    } finally {
      setDeleting(false);
    }
  }

  if (loading) {
    return (
      <main className="news-detail-page">
        <p role="status">{t("Loading news...")}</p>
      </main>
    );
  }

  if (error || !news) {
    return (
      <main className="news-detail-page">
        <Link className="news-detail__back" to="/noticias">
          <ArrowLeft size={16} aria-hidden="true" />
          {t("Back to news")}
        </Link>
        <p className="news-detail__error" role="alert">
          {error || t("News not found.")}
        </p>
      </main>
    );
  }

  const isAuthor =
    currentUser?.username.toLowerCase() === news.authorUsername.toLowerCase();

  return (
    <main className="news-detail-page">
      <Link className="news-detail__back" to="/noticias">
        <ArrowLeft size={16} aria-hidden="true" />
        {t("Back to news")}
      </Link>

      <article className="news-detail">
        {news.imageUrl && (
          <img className="news-detail__image" src={news.imageUrl} alt="" />
        )}

        <div className="news-detail__content">
          <time className="news-detail__date" dateTime={news.publishedAt}>
            {new Date(news.publishedAt).toLocaleDateString("en-US", {
              day: "2-digit",
              month: "long",
              year: "numeric",
            })}
          </time>

          <h1 className="news-detail__title">{news.title}</h1>

          <p className="news-detail__author">
            {t("Published by")}{" "}
            <Link to={`/user/${encodeURIComponent(news.authorUsername)}`}>
              {news.authorUsername}
            </Link>
          </p>

          <p className="news-detail__description">{news.description}</p>

          {isAuthor && (
            <div className="news-detail__actions">
              <Link className="news-detail__edit" to={`/noticias/nueva?edit=${news.id}`}>
                <Pencil size={15} aria-hidden="true" />
                {t("Edit")}
              </Link>

              <button
                className="news-detail__delete"
                type="button"
                onClick={handleDelete}
                disabled={deleting}
              >
                <Trash2 size={15} aria-hidden="true" />
                {deleting ? t("Deleting...") : t("Delete")}
              </button>
            </div>
          )}

          {deleteError && (
            <p className="news-detail__error" role="alert">
              {deleteError}
            </p>
          )}
        </div>
      </article>

      <CommentSection newsId={news.id} />
    </main>
  );
}

export default NewsDetail;
