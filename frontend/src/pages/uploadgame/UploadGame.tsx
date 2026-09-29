import { useEffect, useState, type FormEvent } from "react";
import { Navigate, useLocation } from "react-router-dom";
import {
  getCurrentUser,
  type AuthUser,
} from "../../services/authService";
import { uploadGame } from "../../services/gameUploadService";

function UploadGame() {
  const location = useLocation();

  const [user, setUser] = useState<AuthUser | null>(null);
  const [checkingSession, setCheckingSession] = useState(true);
  const [sessionError, setSessionError] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [archive, setArchive] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => setSessionError("Could not check your session. Please try again."))
      .finally(() => setCheckingSession(false));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!archive) {
      setError("Select a ZIP file containing the game.");
      return;
    }

    setUploading(true);

    try {
      const result = await uploadGame(title.trim(), description.trim(), archive);
      setSuccess(
        `"${result.title}" was uploaded successfully. ${result.filesStored} files stored.`,
      );
      setTitle("");
      setDescription("");
      setArchive(null);
      event.currentTarget.reset();
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

  if (checkingSession) {
    return <main>Checking session...</main>;
  }

  if (sessionError) {
    return <main role="alert">{sessionError}</main>;
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
      <h1>Upload a game</h1>

      <form onSubmit={handleSubmit}>
        <label htmlFor="game-title">Title</label>
        <input
          id="game-title"
          type="text"
          value={title}
          onChange={(event) => setTitle(event.target.value)}
          maxLength={120}
          required
        />

        <label htmlFor="game-description">Description</label>
        <textarea
          id="game-description"
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          maxLength={5000}
          rows={5}
        />

        <label htmlFor="game-archive">Game ZIP</label>
        <input
          id="game-archive"
          type="file"
          accept=".zip,application/zip"
          onChange={(event) => setArchive(event.target.files?.[0] ?? null)}
          required
        />

        {error && <p role="alert">{error}</p>}
        {success && <p role="status">{success}</p>}

        <button type="submit" disabled={uploading}>
          {uploading ? "Uploading..." : "Upload game"}
        </button>
      </form>
    </main>
  );
}

export default UploadGame;