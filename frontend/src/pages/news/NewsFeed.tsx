import { useEffect, useState } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { PlusCircle } from "lucide-react";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { getNewsPage } from "../../services/newsService";
import { useTranslation } from "../../services/i18n";
import NewsCard from "../../components/news/NewsCard";
import type { NewsPage } from "../../types/News";
import "./NewsFeed.css";

const PAGE_SIZE = 9;

function NewsFeed() {
  const { t } = useTranslation();
  const location = useLocation();

  const [user, setUser] = useState<AuthUser | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<NewsPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setCheckingSession(false));
  }, []);

  useEffect(() => {
    if (checkingSession || !user) {
      return;
    }

    let cancelled = false;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- a page change starts a distinct feed request.
    setLoading(true);
    setError("");

    getNewsPage(pageNumber, PAGE_SIZE)
      .then((loaded) => {
        if (!cancelled) setPage(loaded);
      })
      .catch((requestError: unknown) => {
        if (!cancelled) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load news.",
          );
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [checkingSession, user, pageNumber]);

  if (checkingSession) {
    return (
      <main className="news-page">
        <p role="status">{t("Checking session…")}</p>
      </main>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  return (
    <main className="news-page">
      <header className="news-page__header">
        <div>
          <h1 className="news-page__title">{t("News")}</h1>
          <p className="news-page__subtitle">
            {t("Updates from the creators you follow.")}
          </p>
        </div>

        <Link className="news-page__publish" to="/noticias/nueva">
          <PlusCircle size={17} aria-hidden="true" />
          {t("Publish news")}
        </Link>
      </header>

      {loading && <p role="status">{t("Loading news...")}</p>}

      {!loading && error && (
        <p className="news-page__error" role="alert">
          {error}
        </p>
      )}

      {!loading && !error && page?.content.length === 0 && (
        <div className="news-page__empty">
          <h2>{t("Your feed is empty")}</h2>
          <p>{t("Follow creators to see their news here.")}</p>
          <Link to="/search">{t("Find creators")}</Link>
        </div>
      )}

      {!loading && !error && page && page.content.length > 0 && (
        <>
          <div className="news-page__grid">
            {page.content.map((news) => (
              <NewsCard key={news.id} news={news} />
            ))}
          </div>

          {page.totalPages > 1 && (
            <nav className="news-page__pagination" aria-label={t("News pages")}>
              <button
                type="button"
                onClick={() => setPageNumber((current) => current - 1)}
                disabled={!page.hasPrevious || loading}
              >
                {t("Previous")}
              </button>

              <span>
                {t("Page")} {page.page + 1} {t("of")} {page.totalPages}
              </span>

              <button
                type="button"
                onClick={() => setPageNumber((current) => current + 1)}
                disabled={!page.hasNext || loading}
              >
                {t("Next")}
              </button>
            </nav>
          )}
        </>
      )}
    </main>
  );
}

export default NewsFeed;
