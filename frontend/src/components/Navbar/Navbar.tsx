import { useEffect, useState } from "react";
import { NavLink, useNavigate } from "react-router-dom";
import "./Navbar.css";

function Navbar() {
    const [isOpen, setIsOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState("");
    const navigate = useNavigate();

    useEffect(() => {
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                setIsOpen(false);
            }
        };

        if (isOpen) {
            document.addEventListener("keydown", handleKeyDown);
        }

        return () => {
            document.removeEventListener("keydown", handleKeyDown);
        };
    }, [isOpen]);

    const closeNavbar = () => {
        setIsOpen(false);
    };

    const navigateAndClose = (path: string) => {
        navigate(path);
        setIsOpen(false);
    };

    const handleSearchChange = (event: React.ChangeEvent<HTMLInputElement>) => {
        setSearchQuery(event.target.value);
    };

    return (
        <>
            <button
                className="navbar-hamburger"
                type="button"
                aria-label="Open menu"
                onClick={() => setIsOpen(true)}
            >
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M4 6h16M4 12h16M4 18h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                </svg>
            </button>

            <div
                className={`navbar-overlay ${isOpen ? "navbar-overlay--open" : ""}`}
                aria-hidden={!isOpen}
                onClick={closeNavbar}
            />

            <aside className={`navbar-drawer ${isOpen ? "navbar-drawer--open" : ""}`} aria-hidden={!isOpen}>
                <header className="navbar-header">
                    <span className="navbar-brand">Ludoteca</span>
                    <button
                        className="navbar-close"
                        type="button"
                        aria-label="Close menu"
                        onClick={closeNavbar}
                    >
                        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M6 6l12 12M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                        </svg>
                    </button>
                </header>

                <input
                    className="navbar-search"
                    type="search"
                    placeholder="Search games..."
                    value={searchQuery}
                    onChange={handleSearchChange}
                    aria-label="Search games"
                />

                <nav className="navbar-nav" aria-label="Main menu">
                    <NavLink
                        className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`}
                        to="/"
                        end
                        onClick={closeNavbar}
                    >
                        Home
                    </NavLink>
                    <NavLink
                        className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`}
                        to="/profile"
                        onClick={closeNavbar}
                    >
                        Profile
                    </NavLink>
                    <NavLink
                        className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`}
                        to="/settings"
                        onClick={closeNavbar}
                    >
                        Settings
                    </NavLink>
                </nav>

                <div className="navbar-divider" />

                <nav className="navbar-nav" aria-label="Actions">
                    <button
                        className="navbar-button"
                        type="button"
                        onClick={() => navigateAndClose("/randomgame")}
                    >
                        Randomize game
                    </button>
                    <NavLink
                        className={({ isActive }) => `navbar-link ${isActive ? "navbar-link--active" : ""}`}
                        to="/forum"
                        onClick={closeNavbar}
                    >
                        Forum
                    </NavLink>
                </nav>

                <div className="navbar-divider" />

                <button
                    className="navbar-button navbar-button--logout"
                    type="button"
                    onClick={() => navigateAndClose("/logout")}
                >
                    Log out
                </button>
            </aside>
        </>
    );
}

export default Navbar;