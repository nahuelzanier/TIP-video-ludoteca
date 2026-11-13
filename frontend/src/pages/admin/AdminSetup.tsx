import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import { setupFirstAdmin } from "../../services/adminGameService";
import "./AdminSetup.css";

function AdminSetup() {
  const location = useLocation();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [checking, setChecking] = useState(true);
  const [token, setToken] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    getCurrentUser().then(setUser)
      .catch(() => setError("Could not check your session."))
      .finally(() => setChecking(false));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setSubmitting(true);
    try {
      const admin = await setupFirstAdmin(token);
      setToken("");
      setSuccess(`${admin.username} is now the first administrator. You can review submitted games now.`);
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not enable administrator access.");
    } finally {
      setSubmitting(false);
    }
  }

  if (checking) return <main className="admin-setup"><p>Checking session…</p></main>;
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (user.role === "ADMIN") return <Navigate to="/admin/review" replace />;

  return (
    <main className="admin-setup">
      <section className="admin-setup__card">
        <p className="admin-setup__eyebrow">Administrator setup</p>
        <h1>Enable the first admin</h1>
        <p>Sign in with the regular account you want to make the first administrator, then enter the setup key configured for this environment.</p>
        <form onSubmit={(event) => void handleSubmit(event)}>
          <label htmlFor="admin-setup-token">Setup key</label>
          <input id="admin-setup-token" type="password" value={token} onChange={(event) => setToken(event.target.value)} required autoComplete="off" />
          {error && <p className="admin-setup__error" role="alert">{error}</p>}
          {success && <p className="admin-setup__success" role="status">{success} <Link to="/admin/review">Open game review</Link></p>}
          <button type="submit" disabled={submitting}>{submitting ? "Setting up…" : "Enable admin access"}</button>
        </form>
        <p className="admin-setup__note">This setup works only while no administrator exists. Afterward, admins can grant access to other registered accounts from the review page.</p>
        <Link to="/">Back to Ludarium</Link>
      </section>
    </main>
  );
}

export default AdminSetup;
