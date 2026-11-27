import { Link } from "react-router-dom";
import type { NewsItem } from "../../types/News";
import "./NewsCard.css";

const SUMMARY_LENGTH = 180;

interface NewsCardProps {
  news: NewsItem;
}

function NewsCard({ news }: NewsCardProps) {
  const path = `/noticias/${news.id}`;
  const authorPath = `/user/${encodeURIComponent(news.authorUsername)}`;
  const summary =
    news.description.length > SUMMARY_LENGTH
      ? `${news.description.slice(0, SUMMARY_LENGTH).trimEnd()}…`
      : news.description;

  return (
    <article className="news-card">
      {news.imageUrl && (
        <Link className="news-card__image-link" to={path} aria-hidden="true" tabIndex={-1}>
          <img className="news-card__image" src={news.imageUrl} alt="" loading="lazy" />
        </Link>
      )}

      <div className="news-card__body">
        <time className="news-card__date" dateTime={news.publishedAt}>
          {formatDate(news.publishedAt)}
        </time>

        <h2 className="news-card__title">
          <Link className="news-card__title-link" to={path}>
            {news.title}
          </Link>
        </h2>

        <p className="news-card__summary">{summary}</p>

        <Link className="news-card__author" to={authorPath}>
          {news.authorAvatarUrl ? (
            <img className="news-card__avatar" src={news.authorAvatarUrl} alt="" loading="lazy" />
          ) : (
            <span className="news-card__avatar news-card__avatar--fallback" aria-hidden="true">
              {news.authorUsername.charAt(0).toUpperCase()}
            </span>
          )}
          <span>{news.authorUsername}</span>
        </Link>
      </div>
    </article>
  );
}

function formatDate(value: string): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return date.toLocaleDateString("en-US", {
    day: "2-digit",
    month: "short",
    year: "numeric",
  });
}

export default NewsCard;
