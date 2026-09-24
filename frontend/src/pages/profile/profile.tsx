import { useEffect, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import {
  getCurrentUser,
  logout as logoutUser,
  type AuthUser,
} from "../../services/authService";
import "./Profile.css";

function Profile() {
  const location = useLocation();
  const navigate = useNavigate();

  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState("");
  const [logoutError, setLogoutError] = useState("");

  useEffect(() => {
    getCurrentUser()
      .then(setUser)
      .catch(() => setError("Could not load your profile. Please try again."))
      .finally(() => setLoading(false));
  }, []);

  async function handleLogout() {
    setLogoutError("");
    setLoggingOut(true);

    try {
      await logoutUser();
      navigate("/login", { replace: true });
    } catch (requestError) {
      setLogoutError(
        requestError instanceof Error
          ? requestError.message
          : "Could not log out. Please try again.",
      );
    } finally {
      setLoggingOut(false);
    }
  }

  if (loading) {
    return <main className="profile-page">Loading profile...</main>;
  }

  if (error) {
    return <main className="profile-page">{error}</main>;
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
    <main className="profile-page">
      <h1>Profile</h1>
      <p>Username: {user.username}</p>
      <p>Email: {user.email}</p>

      {logoutError && <p role="alert">{logoutError}</p>}

      <button
        className="profile-logout"
        type="button"
        onClick={handleLogout}
        disabled={loggingOut}
      >
        {loggingOut ? "Logging out..." : "Log out"}
      </button>
    </main>
  );
}

export default Profile;