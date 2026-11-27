import { useEffect, useState, type FormEvent } from "react";
import { Dices, Heart, Newspaper, Search } from "lucide-react";
import { NavLink, useLocation, useNavigate } from "react-router-dom";
import {
  getCurrentUser,
  logout as logoutUser,
  type AuthUser,
} from "../../services/authService";
import { getRandomGame } from "../../services/gameService";
import { isFirstAdminSetupAvailable } from "../../services/adminGameService";
import { getUserProfile } from "../../services/userService";
import { useTranslation } from "../../services/i18n";
import "./Navbar.css";

function Navbar() {
  const { t } = useTranslation();
  const [currentUser, setCurrentUser] = useState<AuthUser | null>(null);
  const [avatarUrl, setAvatarUrl] = useState<string | null>(null);
  const [accountLoading, setAccountLoading] = useState(true);
  const [accountError, setAccountError] = useState("");
  const [isOpen, setIsOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState("");
  const [pickingRandom, setPickingRandom] = useState(false);
  const [randomError, setRandomError] = useState("");
  const [adminSetupAvailable, setAdminSetupAvailable] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const location = useLocation();
  const navigate = useNavigate();

  useEffect(() => {
    let isCurrentRequest = true;

    // eslint-disable-next-line react-hooks/set-state-in-effect -- route changes revalidate the session and profile avatar.
    setAccountLoading(true);
    setAccountError("");

    getCurrentUser()
      .then(async (user) => {
        if (!isCurrentRequest) return;
        setCurrentUser(user);
        setAvatarUrl(null);

        if (user && user.role !== "ADMIN") {
          const available = await isFirstAdminSetupAvailable().catch(() => false);
          if (isCurrentRequest) setAdminSetupAvailable(available);
        } else {
          setAdminSetupAvailable(false);
        }

        if (user) {
          try {
            const profile = await getUserProfile(user.username);
            if (isCurrentRequest) setAvatarUrl(profile?.avatarUrl ?? null);
          } catch {
            if (isCurrentRequest) setAccountError("Could not load your profile picture.");
          }
        }
      })
      .catch((requestError: unknown) => {
        if (isCurrentRequest) {
          setAccountError(
            requestError instanceof Error
              ? requestError.message
              : "Could not load your account.",
          );
        }
      })
      .finally(() => {
        if (isCurrentRequest) setAccountLoading(false);
      });

    return () => {
      isCurrentRequest = false;
    };
  }, [location.pathname]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsOpen(false);
    };

    if (isOpen) document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [isOpen]);

  const closeNavbar = () => setIsOpen(false);

  const handleSearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const term = searchQuery.trim();
    navigate(term ? `/search?q=${encodeURIComponent(term)}` : "/search");
    closeNavbar();
  };

  const handleLogout = async () => {
    setLogoutError("");
    setLoggingOut(true);

    try {
      await logoutUser();
      setCurrentUser(null);
      setAvatarUrl(null);
      setIsOpen(false);
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
  };

  const handleRandomGame = async () => {
    setRandomError("");
    setPickingRandom(true);

    try {
      const game = await getRandomGame();
      navigate(`/game/${encodeURIComponent(game.id)}`);
      setIsOpen(false);
    } catch (requestError) {
      setRandomError(
        requestError instanceof Error
          ? requestError.message
          : "Could not load a random game.",
      );
    } finally {
      setPickingRandom(false);
    }
  };

  return (
    <>
      <header className="site-header">
        <div className="site-header__inner">
          <NavLink className="site-header__brand" to="/" aria-label="Ludarium, home">
            <svg className="site-header__logo" viewBox="0 0 40 44" fill="none" aria-hidden="true">
              <path d="M20 2.5 36.5 12v20L20 41.5 3.5 32V12L20 2.5Z" stroke="currentColor" strokeWidth="3.5" />
              <path d="m12 18 8-4.5 8 4.5v8l-8 4.5-8-4.5v-8Z" fill="#ed4f88" />
              <path d="m12 18 8 4.5 8-4.5v8l-8 4.5-8-4.5v-8Z" fill="#c52d72" />
            </svg>
            <span className="site-header__brand-copy">
              <strong>Ludarium</strong>
            <small>{t("Your space to play")}</small>
            </span>
          </NavLink>

          <nav className="site-header__nav" aria-label={t("Main navigation")}>
            <NavLink to="/" end>{t("Explore")}</NavLink>
            <NavLink className="site-header__nav-with-icon" to="/noticias">
              <Newspaper size={15} aria-hidden="true" />
              {t("News")}
            </NavLink>
            <NavLink to="/forum">{t("Community")}</NavLink>
            {currentUser && (
              <NavLink className="site-header__nav-with-icon" to="/favorites">
                <Heart size={15} aria-hidden="true" />
                {t("My favorites")}
              </NavLink>
            )}
            <button
              className="site-header__random"
              type="button"
              onClick={handleRandomGame}
              disabled={pickingRandom}
            >
              <Dices size={15} aria-hidden="true" />
              {pickingRandom ? t("Finding a game...") : t("Random game")}
            </button>
            <NavLink className="site-header__upload" to="/games/upload">{t("Upload a game")}</NavLink>
            {currentUser?.role === "ADMIN" && <NavLink to="/admin/review">{t("Review games")}</NavLink>}
            {adminSetupAvailable && <NavLink to="/admin/setup">{t("Set up first admin")}</NavLink>}
          </nav>

          <form className="site-header__search" role="search" onSubmit={handleSearch}>
            <label className="visually-hidden" htmlFor="navbar-search">{t("Search games")}</label>
            <input
              id="navbar-search"
              type="search"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              placeholder={t("Search games, tags, or creators...")}
            />
            <button type="submit" aria-label="Search">
              <Search size={16} aria-hidden="true" />
            </button>
          </form>

          {accountLoading ? (
              <span className="site-header__account-loading" role="status">{t("Loading account...")}</span>
          ) : currentUser ? (
            <div className="site-header__account-group">
              <NavLink
                className="site-header__account"
                to={`/user/${encodeURIComponent(currentUser.username)}`}
                aria-label={`${t("View profile")}: ${currentUser.username}`}
                title={currentUser.username}
              >
                {avatarUrl ? (
                  <img
                    src={avatarUrl}
                    alt=""
                    onError={() => {
                      setAvatarUrl(null);
                      setAccountError("Could not load your profile picture.");
                    }}
                  />
                ) : (
                  <span aria-hidden="true">{currentUser.username.charAt(0).toUpperCase()}</span>
                )}
              </NavLink>
              {accountError && (
                <span className="site-header__account-error" role="status">
                  {t("Profile picture unavailable")}
                </span>
              )}
            </div>
          ) : accountError && !currentUser ? (
            <span className="site-header__account-error" role="alert">{t("Account unavailable")}</span>
          ) : (
            <div className="site-header__auth">
              <NavLink to="/login">{t("Log in")}</NavLink>
              <NavLink className="site-header__register" to="/register">{t("Sign up")}</NavLink>
            </div>
          )}

          <button
            className="navbar-hamburger"
            type="button"
            aria-label={t("Open menu")}
            aria-expanded={isOpen}
            onClick={() => setIsOpen(true)}
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </div>
        {randomError && (
          <p className="site-header__random-error" role="alert">
            {randomError}
          </p>
        )}
      </header>

      <div
        className={`navbar-overlay ${isOpen ? "navbar-overlay--open" : ""}`}
        aria-hidden={!isOpen}
        onClick={closeNavbar}
      />

      <aside className={`navbar-drawer ${isOpen ? "navbar-drawer--open" : ""}`} aria-hidden={!isOpen}>
        <header className="navbar-header">
          <span className="navbar-brand">Ludarium</span>
          <button className="navbar-close" type="button" aria-label="Close menu" onClick={closeNavbar}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
          </button>
        </header>

        <nav className="navbar-nav" aria-label={t("Main menu")}>
          <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to="/" end onClick={closeNavbar}>{t("Explore")}</NavLink>
          <NavLink className={({ isActive }) => `navbar-link navbar-link--with-icon ${isActive ? "navbar-link--active" : ""}`} to="/noticias" onClick={closeNavbar}>
            <Newspaper size={17} aria-hidden="true" />
            {t("News")}
          </NavLink>
          {currentUser && (
            <>
              <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to={`/user/${encodeURIComponent(currentUser.username)}`} onClick={closeNavbar}>{t("My profile")}</NavLink>
              <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to="/settings" onClick={closeNavbar}>{t("Settings")}</NavLink>
              <NavLink className={({ isActive }) => `navbar-link navbar-link--with-icon ${isActive ? "navbar-link--active" : ""}`} to="/favorites" onClick={closeNavbar}>
                <Heart size={17} aria-hidden="true" />
                {t("My favorites")}
              </NavLink>
              {currentUser.role === "ADMIN" && <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to="/admin/review" onClick={closeNavbar}>{t("Review games")}</NavLink>}
              {adminSetupAvailable && <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to="/admin/setup" onClick={closeNavbar}>{t("Set up first admin")}</NavLink>}
            </>
          )}
        </nav>

        <div className="navbar-divider" />

        <nav className="navbar-nav" aria-label={t("Actions")}>
          <button className="navbar-button" type="button" onClick={handleRandomGame} disabled={pickingRandom}>
            {pickingRandom ? t("Finding a game...") : t("Random game")}
          </button>
          <NavLink className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`} to="/forum" onClick={closeNavbar}>{t("Community")}</NavLink>
          <NavLink className="navbar-link" to="/games/upload" onClick={closeNavbar}>{t("Upload a game")}</NavLink>
        </nav>

        {randomError && <p className="navbar-error" role="alert">{randomError}</p>}

        <div className="navbar-divider" />

        {!currentUser && !accountLoading && !accountError && (
          <div className="navbar-nav navbar-nav--auth">
          <NavLink className="navbar-link" to="/login" onClick={closeNavbar}>{t("Log in")}</NavLink>
            <NavLink className="navbar-link" to="/register" onClick={closeNavbar}>{t("Sign up")}</NavLink>
          </div>
        )}

        {currentUser && (
          <button className="navbar-button navbar-button--logout" type="button" onClick={handleLogout} disabled={loggingOut}>
            {loggingOut ? t("Logging out...") : t("Log out")}
          </button>
        )}

        {logoutError && <p className="navbar-error" role="alert">{logoutError}</p>}
      </aside>
    </>
  );
}

export default Navbar;
