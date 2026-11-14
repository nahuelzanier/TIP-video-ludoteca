import { useState, type FormEvent } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { login, register } from "../services/authService";
import { useTranslation } from "../services/i18n";
import "./AuthPage.css";

interface AuthPageProps {
  mode: "login" | "register";
}

function AuthPage({ mode }: AuthPageProps) {
  const { t } = useTranslation();
  const isRegister = mode === "register";

  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();
  const location = useLocation();

  const returnTo =
    (location.state as { from?: string } | null)?.from ?? "/profile";

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");

    if (isRegister && password !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);

    try {
      if (isRegister) {
        await register(username.trim(), email.trim(), password);
      }

      await login(email.trim(), password);
      navigate(returnTo, { replace: true });
    } catch (requestError) {
      setError(
        requestError instanceof Error
          ? requestError.message
          : "Something went wrong. Please try again.",
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card">
        <Link className="auth-back" to="/">
          ← {t("Back to Ludarium")}
        </Link>

        <h1>{t(isRegister ? "Create account" : "Log in")}</h1>

        <p className="auth-description">
          {isRegister
            ? t("Create an account to join the community.")
            : t("Log in to your Ludarium account.")}
        </p>

        <form className="auth-form" onSubmit={handleSubmit}>
          {isRegister && (
            <label>
              {t("Username")}
              <input
                type="text"
                value={username}
                onChange={(event) => setUsername(event.target.value)}
                autoComplete="username"
                minLength={3}
                maxLength={30}
                pattern="[A-Za-z0-9_]+"
                title={t("Use letters, numbers, or underscores.")}
                required
              />
            </label>
          )}

          <label>
            {t("Email address")}
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              autoComplete="email"
              required
            />
          </label>

          <label>
            {t("Password")}
            <input
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              autoComplete={isRegister ? "new-password" : "current-password"}
              minLength={isRegister ? 8 : undefined}
              maxLength={72}
              required
            />
          </label>

          {isRegister && (
            <label>
              {t("Confirm password")}
              <input
                type="password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                autoComplete="new-password"
                minLength={8}
                maxLength={72}
                required
              />
            </label>
          )}

          {error && <p className="auth-feedback auth-error">{t(error)}</p>}
          {message && <p className="auth-feedback auth-success">{t(message)}</p>}

          <button className="auth-submit" type="submit" disabled={loading}>
            {loading
              ? t("Please wait...")
              : isRegister
                ? t("Create account")
                : t("Log in")}
          </button>
        </form>

        <p className="auth-switch">
          {t(isRegister ? "Already have an account?" : "Don't have an account yet?")}{" "}
          <Link to={isRegister ? "/login" : "/register"}>
            {t(isRegister ? "Log in" : "Sign up")}
          </Link>
        </p>
      </section>
    </main>
  );
}

export default AuthPage;
