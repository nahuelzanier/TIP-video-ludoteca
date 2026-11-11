import { useCallback, useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { getDraftGames, promoteUserToAdmin, publishDraft, type ReviewGame } from "../../services/adminGameService";
import { API_BASE_URL, resolveAssetUrl } from "../../services/api";
import "./AdminReview.css";

function AdminReview() {
  const location = useLocation();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [checking, setChecking] = useState(true);
  const [games, setGames] = useState<ReviewGame[]>([]);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [newAdminEmail, setNewAdminEmail] = useState("");
  const [promoting, setPromoting] = useState(false);

  const loadDrafts = useCallback(async () => {
    setError("");
    try {
      setGames(await getDraftGames());
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not load drafts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    getCurrentUser().then(setUser)
      .catch(() => setError("Could not check your session."))
      .finally(() => setChecking(false));
  }, []);

  useEffect(() => {
    if (user?.role === "ADMIN") void loadDrafts();
  }, [user, loadDrafts]);

  async function handlePublish(game: ReviewGame) {
    setBusyId(game.id);
    setError("");
    setMessage("");
    try {
      await publishDraft(game.id);
      setGames((current) => current.filter((item) => item.id !== game.id));
      setMessage(`“${game.title}” is published and now visible in the catalog.`);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not publish the game.");
    } finally {
      setBusyId("");
    }
  }

  async function handlePromoteAdmin(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");
    setPromoting(true);
    try {
      const admin = await promoteUserToAdmin(newAdminEmail.trim());
      setNewAdminEmail("");
      setMessage(`${admin.username} (${admin.email}) has admin access. They need to sign out and back in for it to take effect.`);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not grant administrator access.");
    } finally {
      setPromoting(false);
    }
  }

  if (checking) return <main className="admin-review"><p>Checking session…</p></main>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role !== "ADMIN") return <main className="admin-review"><p role="alert">This page is only available to administrators.</p><p><Link to="/admin/setup">Set up the first administrator</Link></p></main>;

  return (
    <main className="admin-review">
      <header className="admin-review__header">
        <p className="admin-review__eyebrow">Admin tools</p>
        <h1>Game review</h1>
        <p>Review each submitted game. Open it when you are ready to test; publishing makes it visible to everyone.</p>
      </header>
      {error && <p className="admin-review__message admin-review__message--error" role="alert">{error}</p>}
      {message && <p className="admin-review__message" role="status">{message}</p>}
      <section className="admin-review__manage">
        <h2>Add an administrator</h2>
        <p>First, the person must register a normal account. Enter the email they used to sign up.</p>
        <form onSubmit={(event) => void handlePromoteAdmin(event)}>
          <label htmlFor="new-admin-email">Registered account email</label>
          <input id="new-admin-email" type="email" value={newAdminEmail} onChange={(event) => setNewAdminEmail(event.target.value)} required maxLength={254} />
          <button type="submit" disabled={promoting}>{promoting ? "Adding…" : "Grant admin access"}</button>
        </form>
      </section>
      {loading ? <p>Loading drafts…</p> : games.length === 0 ? (
        <section className="admin-review__empty"><h2>No drafts waiting</h2><p>New game submissions will appear here for testing.</p></section>
      ) : (
        <>
        <h2 className="admin-review__section-title">Pending games <span>{games.length}</span></h2>
        <div className="admin-review__list">
          {games.map((game) => (
            <article className="admin-review__card" key={game.id}>
              <img className="admin-review__cover" src={resolveAssetUrl(game.image) ?? undefined} alt={`Cover for ${game.title}`} />
              <div className="admin-review__card-content">
                <p className="admin-review__eyebrow">Pending review</p>
                <h3>{game.title}</h3>
                <p className="admin-review__description">{game.description || "No description provided."}</p>
                <p className="admin-review__meta">By {game.ownerUsername} · {new Date(game.createdAt).toLocaleDateString("es-AR")}</p>
                <div className="admin-review__actions">
                  <a className="admin-review__test" href={`${API_BASE_URL}/games/${encodeURIComponent(game.id)}/index.html`} target="_blank" rel="noreferrer">Test game</a>
                  <button type="button" onClick={() => void handlePublish(game)} disabled={busyId !== ""}>
                    {busyId === game.id ? "Publishing…" : "Approve and publish"}
                  </button>
                </div>
              </div>
            </article>
          ))}
        </div>
        </>
      )}
    </main>
  );
}

export default AdminReview;
