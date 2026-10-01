import { useEffect, useRef, useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import {
  getCurrentUser,
  type AuthUser,
} from "../../services/authService";
import { publishGame, uploadGame } from "../../services/gameUploadService";
import "./UploadGame.css";

const MAX_COVER_SIZE = 5 * 1024 * 1024;

function UploadGame() {
  const location = useLocation();
  const formRef = useRef<HTMLFormElement>(null);

  const [user, setUser] = useState<AuthUser | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionError, setSessionError] = useState("");

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [cover, setCover] = useState<File | null>(null);
  const [archive, setArchive] = useState<File | null>(null);
  const [coverPreview, setCoverPreview] = useState("");

  const [uploading, setUploading] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [lastUpload, setLastUpload] = useState<{
    id: string;
    title: string;
  } | null>(null);
  const [published, setPublished] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() =>
        setSessionError("Could not check your session. Please try again."),
      )
      .finally(() => setCheckingSession(false));
  }, []);

  useEffect(() => {
    if (!cover) {
      setCoverPreview("");
      return;
    }

    const previewUrl = URL.createObjectURL(cover);
    setCoverPreview(previewUrl);

    return () => URL.revokeObjectURL(previewUrl);
  }, [cover]);

  function handleCoverChange(file: File | null) {
    setError("");
    setCover(null);

    if (!file) return;

    if (!["image/png", "image/jpeg"].includes(file.type)) {
      setError("The cover must be a PNG or JPEG image.");
      return;
    }

    if (file.size > MAX_COVER_SIZE) {
      setError("The cover image cannot exceed 5 MB.");
      return;
    }

    setCover(file);
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!cover || !archive) {
      setError("Choose a cover image and a ZIP file containing the game.");
      return;
    }

    const form = formRef.current;
    setUploading(true);

    try {
      const result = await uploadGame(
        title.trim(),
        description.trim(),
        cover,
        archive,
      );

      setSuccess(
        `"${result.title}" was uploaded. ${result.filesStored} files were stored as a draft.`,
      );
      setLastUpload({ id: result.id, title: result.title });
      setPublished(false);

      setTitle("");
      setDescription("");
      setCover(null);
      setArchive(null);
      form?.reset();
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not upload the game. Please try again.",
      );
    } finally {
      setUploading(false);
    }
  }

  async function handlePublish() {
    if (!lastUpload) return;

    setError("");
    setPublishing(true);

    try {
      const result = await publishGame(lastUpload.id);

      setPublished(true);
      setSuccess(
        `"${result.title}" is now published and visible on the home page.`,
      );
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Could not publish the game. Please try again.",
      );
    } finally {
      setPublishing(false);
    }
  }

  if (checkingSession) {
    return (
      <main className="upload-game-page">
        <p>Checking session...</p>
      </main>
    );
  }

  if (sessionError) {
    return (
      <main className="upload-game-page">
        <p role="alert">{sessionError}</p>
      </main>
    );
  }

  if (!user) {
    return (
      <Navigate
        to="/login"
        replace
        state={{ from: location.pathname }}
      />
    );
  }

  return (
    <main className="upload-game-page">
      <section className="upload-game-card">
        <header className="upload-game-header">
          <p className="upload-game-eyebrow">Creator tools</p>
          <h1>Upload a game</h1>
          <p>
            Add the game details, a cover image, and a ZIP containing its web
            export.
          </p>
        </header>

        <form ref={formRef} className="upload-game-form" onSubmit={handleSubmit}>
          <label htmlFor="game-title">Game title</label>
          <input
            id="game-title"
            name="title"
            type="text"
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={120}
            placeholder="Enter your game's title"
            required
          />

          <label htmlFor="game-description">Description</label>
          <textarea
            id="game-description"
            name="description"
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            maxLength={5000}
            rows={5}
            placeholder="Tell players what your game is about"
          />

          <label htmlFor="game-cover">Cover image</label>
          <p className="upload-game-help">
            PNG or JPEG, up to 5 MB.
          </p>
          <input
            id="game-cover"
            name="cover"
            type="file"
            accept="image/png,image/jpeg"
            onChange={(event) =>
              handleCoverChange(event.target.files?.[0] ?? null)
            }
            required
          />

          {coverPreview && (
            <figure className="upload-game-preview">
              <img src={coverPreview} alt={`Cover preview for ${title || "your game"}`} />
              <figcaption>Cover preview</figcaption>
            </figure>
          )}

          <label htmlFor="game-archive">Game files</label>
          <p className="upload-game-help">
            Choose a ZIP with <code>index.html</code> at its root. Maximum ZIP
            size: 100 MB.
          </p>
          <input
            id="game-archive"
            name="archive"
            type="file"
            accept=".zip,application/zip"
            onChange={(event) =>
              setArchive(event.target.files?.[0] ?? null)
            }
            required
          />

          {archive && (
            <p className="upload-game-file-name">
              Selected: {archive.name}
            </p>
          )}

          {error && <p className="upload-game-message error" role="alert">{error}</p>}
          {success && (
            <p className="upload-game-message success" role="status">
              {success}
            </p>
          )}

          {lastUpload && !published && (
            <button
              className="upload-game-publish"
              type="button"
              onClick={handlePublish}
              disabled={publishing}
            >
              {publishing ? "Publishing..." : "Publish game"}
            </button>
          )}

          <button
            className="upload-game-submit"
            type="submit"
            disabled={uploading}
          >
            {uploading ? "Uploading..." : "Upload game"}
          </button>
        </form>
      </section>
    </main>
  );
}

export default UploadGame;