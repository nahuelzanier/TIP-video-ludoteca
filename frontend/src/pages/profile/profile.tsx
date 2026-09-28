import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { getCurrentUser, type AuthUser } from "../../services/authService";
import "./profile.css";

function Profile() {
    const location = useLocation();

    const [user, setUser] = useState<AuthUser | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        getCurrentUser()
            .then(setUser)
            .catch(() => setError("No se pudo cargar tu perfil."))
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <main className="profile-page">
                <p className="profile-status" role="status">
                    Cargando perfil...
                </p>
            </main>
        );
    }

    if (error) {
        return (
            <main className="profile-page">
                <p className="profile-status" role="alert">
                    {error}
                </p>
            </main>
        );
    }

    if (!user) {
        return (
            <Navigate to="/login" replace state={{ from: location.pathname }} />
        );
    }

    return <Navigate to={`/user/${user.username}`} replace />;
}

export default Profile;
