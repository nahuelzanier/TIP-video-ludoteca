import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate, useSearchParams } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import {
  createNews,
  getNews,
  MAX_NEWS_DESCRIPTION_LENGTH,
  MAX_NEWS_IMAGE_BYTES,
  MAX_NEWS_TITLE_LENGTH,
  updateNews,
} from "../../services/newsService";
import { useTranslation } from "../../services/i18n";
import "./NewsPublish.css";

function NewsPublish() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const editParam = searchParams.get("edit");
  const editId = editParam === null ? null : Number(editParam);
  const isEditing = editParam !== null;
  const validEditId = editId !== null && Number.isInteger(editId) && editId > 0;

  const [user, setUser] = useState<AuthUser | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [image, setImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState("");
  const [loadError, setLoadError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const previewRef = useRef<string | null>(null);

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setCheckingSession(false));
  }, []);

  useEffect(() => {
    if (editParam === null || !user) {
      return;
    }

    if (!validEditId) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- invalid edit IDs resolve without a request.
      setLoadError("News not found.");
      return;
    }

    let isCurrentRequest = true;

    getNews(editId)
      .then((loaded) => {
        if (!isCurrentRequest) return;

        if (loaded.authorUsername.toLowerCase() !== user.username.toLowerCase()) {
          setLoadError("Only the author can edit this news.");
          return;
        }

        setTitle(loaded.title);
        setDescription(loaded.description);
      })
      .catch((requestError: unknown) => {
        if (!isCurrentRequest) {
          return;
        }

        setLoadError(
          requestError instanceof Error
            ? requestError.message
            : "Could not load this news.",
        );
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [editId, editParam, validEditId, user]);

  useEffect(() => {
    return () => {
      if (previewRef.current) {
        URL.revokeObjectURL(previewRef.current);
      }
    };
  }, []);

  function handleImageChange(file: File | null) {
    if (previewRef.current) {
      URL.revokeObjectURL(previewRef.current);
    }

    previewRef.current = file ? URL.createObjectURL(file) : null;
    setImagePreview(previewRef.current ?? "");
    setImage(file);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (saving) {
      return;
    }

    const cleanTitle = title.trim();
    const cleanDescription = description.trim();

    if (!cleanTitle || !cleanDescription) {
      setSaveError(t("Title and description are required."));
      return;
    }

    if (image && image.size > MAX_NEWS_IMAGE_BYTES) {
      setSaveError(t("The image cannot be larger than 5 MB."));
      return;
    }

    setSaving(true);
    setSaveError("");

    try {
      const payload = { title: cleanTitle, description: cleanDescription, image };
      const saved = isEditing && validEditId
        ? await updateNews(editId, payload)
        : await createNews(payload);

      navigate(`/noticias/${saved.id}`);
    } catch (requestError) {
      setSaveError(
        requestError instanceof Error
          ? requestError.message
          : "Could not save this news.",
      );
    } finally {
      setSaving(false);
    }
  }

  if (checkingSession) {
    return (
      <main className="news-publish-page">
        <p role="status">{t("Checking session…")}</p>
      </main>
    );
  }

  if (!user) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  return (
    <main className="news-publish-page">
      <Link className="news-publish__back" to="/noticias">
        <span aria-hidden="true">←</span>
        {t("Back to news")}
      </Link>

      <div className="news-publish__panel">
        <h1>{isEditing ? t("Edit news") : t("Publish news")}</h1>
        <p className="news-publish__intro">
          {isEditing
            ? t("Update the title, description, or image.")
            : t("Share an update with the creators who follow you.")}
        </p>

        {loadError ? (
          <div className="news-publish__error" role="alert">
            <p>{loadError}</p>
            <Link to="/noticias">{t("Back to news")}</Link>
          </div>
        ) : (
          <form className="news-publish__form" onSubmit={handleSubmit}>
            <label htmlFor="news-title">{t("Title")}</label>
            <input
              id="news-title"
              value={title}
              maxLength={MAX_NEWS_TITLE_LENGTH}
              onChange={(event) => setTitle(event.target.value)}
              placeholder={t("A clear headline for your news")}
              required
            />

            <label htmlFor="news-description">{t("Description")}</label>
            <textarea
              id="news-description"
              value={description}
              maxLength={MAX_NEWS_DESCRIPTION_LENGTH}
              rows={8}
              onChange={(event) => setDescription(event.target.value)}
              placeholder={t("Tell the community what is new...")}
              required
            />

            <label htmlFor="news-image">{t("News image")}</label>
            <input
              id="news-image"
              type="file"
              accept="image/png,image/jpeg,image/webp"
              onChange={(event) => handleImageChange(event.target.files?.[0] ?? null)}
            />
            <p className="news-publish__hint">
              {t("Optional. PNG, JPEG or WEBP, up to 5 MB.")}
            </p>

            {imagePreview && (
              <img className="news-publish__preview" src={imagePreview} alt={t("Image preview")} />
            )}

            {saveError && (
              <p className="news-publish__error-text" role="alert">
                {saveError}
              </p>
            )}

            <div className="news-publish__actions">
              <button type="submit" disabled={saving}>
                {saving
                  ? t("Saving...")
                  : isEditing
                    ? t("Save changes")
                    : t("Publish")}
              </button>
              <Link to="/noticias">{t("Cancel")}</Link>
            </div>
          </form>
        )}
      </div>
    </main>
  );
}

export default NewsPublish;
