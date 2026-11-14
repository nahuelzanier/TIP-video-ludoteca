import { useEffect, useState, type FormEvent } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import {
  Globe2,
  LockKeyhole,
  LogOut,
  Moon,
  Save,
  Settings as SettingsIcon,
  UserRound,
  Volume2,
} from "lucide-react";
import { getCurrentUser, logout, type AuthUser } from "../../services/authService";
import {
  applyUserSettings,
  getUserSettings,
  saveUserSettings,
  updatePassword,
  updateUsername,
  type UserSettings as UserSettingsValue,
} from "../../services/settingsService";
import "./Settings.css";
import { useTranslation } from "../../services/i18n";

const DEFAULT_SETTINGS: UserSettingsValue = {
  volume: 100,
  language: "en",
  theme: "system",
};

function Settings() {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [settings, setSettings] = useState<UserSettingsValue>(DEFAULT_SETTINGS);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [savingPreferences, setSavingPreferences] = useState(false);
  const [savingUsername, setSavingUsername] = useState(false);
  const [savingPassword, setSavingPassword] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [username, setUsername] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  useEffect(() => {
    let isCurrentRequest = true;
    getCurrentUser()
      .then(async (currentUser) => {
        if (!isCurrentRequest) return;
        setUser(currentUser);
        if (!currentUser) return;
        setUsername(currentUser.username);
        const loadedSettings = await getUserSettings();
        if (!isCurrentRequest) return;
        setSettings(loadedSettings);
        applyUserSettings(loadedSettings);
      })
      .catch((requestError: unknown) => {
        if (isCurrentRequest) {
          const message = requestError instanceof Error ? requestError.message : "Could not load settings.";
          setLoadError(message);
          setError(message);
        }
      })
      .finally(() => {
        if (isCurrentRequest) setLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, []);

  async function handlePreferencesSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setSavingPreferences(true);
    try {
      const saved = await saveUserSettings(settings);
      setSettings(saved);
      applyUserSettings(saved);
      setSuccess(t("Preferences saved to your account."));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not save preferences.");
    } finally {
      setSavingPreferences(false);
    }
  }

  async function handleUsernameSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    setSavingUsername(true);
    try {
      const result = await updateUsername(username.trim());
      setUser((current) => current ? { ...current, username: result.username } : current);
      setUsername(result.username);
      setSuccess(t("Username updated."));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not update username.");
    } finally {
      setSavingUsername(false);
    }
  }

  async function handlePasswordSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setSuccess("");
    if (newPassword !== confirmPassword) {
      setError(t("The new passwords do not match."));
      return;
    }

    setSavingPassword(true);
    try {
      await updatePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");
      setSuccess(t("Password updated."));
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not update password.");
    } finally {
      setSavingPassword(false);
    }
  }

  async function handleLogout() {
    setError("");
    setLoggingOut(true);
    try {
      await logout();
      navigate("/login", { replace: true });
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "Could not log out.");
      setLoggingOut(false);
    }
  }

  if (loading) {
    return <main className="settings-page"><p role="status">{t("Loading settings…")}</p></main>;
  }
  if (loadError) {
    return <main className="settings-page"><p className="settings-message settings-message--error" role="alert">{loadError}</p></main>;
  }
  if (error && !user) {
    return <main className="settings-page"><p className="settings-message settings-message--error" role="alert">{error}</p></main>;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return (
    <main className="settings-page">
      <header className="settings-header">
        <div className="settings-header__icon" aria-hidden="true"><SettingsIcon size={22} /></div>
        <div>
          <p className="settings-eyebrow">{t("Your account")}</p>
          <h1>{t("Settings")}</h1>
          <p>{t("Manage your preferences and account access.")}</p>
        </div>
      </header>

      {error && <p className="settings-message settings-message--error" role="alert">{error}</p>}
      {success && <p className="settings-message settings-message--success" role="status">{success}</p>}

      <section className="settings-section" aria-labelledby="settings-preferences-title">
        <div className="settings-section__heading">
          <div className="settings-section__icon" aria-hidden="true"><Moon size={19} /></div>
          <div><h2 id="settings-preferences-title">{t("Preferences")}</h2><p>{t("These preferences follow your account across devices.")}</p></div>
        </div>
        <form className="settings-form" onSubmit={(event) => void handlePreferencesSubmit(event)}>
          <label className="settings-volume" htmlFor="settings-volume">
            <span className="settings-field-label"><Volume2 size={17} aria-hidden="true" /> {t("Game volume")}</span>
            <span className="settings-volume__control">
              <input
                id="settings-volume"
                type="range"
                min="0"
                max="100"
                value={settings.volume}
                onChange={(event) => setSettings((current) => ({ ...current, volume: Number(event.target.value) }))}
              />
              <output htmlFor="settings-volume">{settings.volume}%</output>
            </span>
          </label>

          <label className="settings-field" htmlFor="settings-language">
            <span className="settings-field-label"><Globe2 size={17} aria-hidden="true" /> {t("Language")}</span>
            <select id="settings-language" value={settings.language} onChange={(event) => setSettings((current) => ({ ...current, language: event.target.value as UserSettingsValue["language"] }))}>
              <option value="en">{t("English")}</option>
              <option value="es">Español</option>
            </select>
          </label>

          <label className="settings-field" htmlFor="settings-theme">
            <span className="settings-field-label"><Moon size={17} aria-hidden="true" /> {t("Theme")}</span>
            <select id="settings-theme" value={settings.theme} onChange={(event) => setSettings((current) => ({ ...current, theme: event.target.value as UserSettingsValue["theme"] }))}>
              <option value="system">{t("Use system setting")}</option>
              <option value="light">{t("Light")}</option>
              <option value="dark">{t("Dark")}</option>
            </select>
          </label>

          <button className="settings-primary-button" type="submit" disabled={savingPreferences}>
            <Save size={16} aria-hidden="true" /> {savingPreferences ? t("Saving…") : t("Save preferences")}
          </button>
        </form>
      </section>

      <section className="settings-section" aria-labelledby="settings-account-title">
        <div className="settings-section__heading">
          <div className="settings-section__icon" aria-hidden="true"><UserRound size={19} /></div>
          <div><h2 id="settings-account-title">{t("Account")}</h2><p>{t("Update the account information stored on your profile.")}</p></div>
        </div>

        <div className="settings-account-summary">
          <span>{t("Email")}</span><strong>{user.email}</strong>
          <Link to={`/user/${encodeURIComponent(user.username)}`}>{t("Manage profile")}</Link>
        </div>

        <form className="settings-form settings-form--compact" onSubmit={(event) => void handleUsernameSubmit(event)}>
          <label className="settings-field" htmlFor="settings-username">
            <span className="settings-field-label">{t("Username")}</span>
            <input id="settings-username" value={username} onChange={(event) => setUsername(event.target.value)} minLength={3} maxLength={30} pattern="[A-Za-z0-9_]{3,30}" autoComplete="username" required />
            <small>{t("3–30 letters, numbers, or underscores.")}</small>
          </label>
          <button className="settings-secondary-button" type="submit" disabled={savingUsername || username.trim() === user.username}>
            {savingUsername ? t("Saving…") : t("Update username")}
          </button>
        </form>

        <form className="settings-form settings-form--password" onSubmit={(event) => void handlePasswordSubmit(event)}>
          <h3><LockKeyhole size={17} aria-hidden="true" /> {t("Change password")}</h3>
          <label className="settings-field" htmlFor="settings-current-password">
            <span className="settings-field-label">{t("Current password")}</span>
            <input id="settings-current-password" type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} autoComplete="current-password" required />
          </label>
          <label className="settings-field" htmlFor="settings-new-password">
            <span className="settings-field-label">{t("New password")}</span>
            <input id="settings-new-password" type="password" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} minLength={8} maxLength={72} autoComplete="new-password" required />
          </label>
          <label className="settings-field" htmlFor="settings-confirm-password">
            <span className="settings-field-label">{t("Confirm new password")}</span>
            <input id="settings-confirm-password" type="password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} minLength={8} maxLength={72} autoComplete="new-password" required />
          </label>
          <button className="settings-secondary-button" type="submit" disabled={savingPassword}>
            {savingPassword ? t("Updating…") : t("Update password")}
          </button>
        </form>
      </section>

      <section className="settings-section settings-section--session" aria-labelledby="settings-session-title">
        <div><h2 id="settings-session-title">{t("Session")}</h2><p>{t("Sign out of Ludarium on this device.")}</p></div>
        <button className="settings-logout-button" type="button" onClick={() => void handleLogout()} disabled={loggingOut}>
          <LogOut size={17} aria-hidden="true" /> {loggingOut ? t("Signing out…") : t("Log out")}
        </button>
      </section>
    </main>
  );
}

export default Settings;
