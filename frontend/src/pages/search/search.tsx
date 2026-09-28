import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import GameGrid from "../../components/games/GameGrid";
import { searchEverything } from "../../services/searchService";
import type { PageResult, SearchResponse, SearchUser } from "../../types/Search";
import "./search.css";

type SectionKey = "gamesPage" | "usersPage";

interface PaginationProps {
    label: string;
    page: PageResult<unknown>;
    onChange: (page: number) => void;
}

interface SearchState {
    term: string;
    key: string;
    results: SearchResponse | null;
    error: string;
}

const MIN_TERM_LENGTH = 2;

function readPage(value: string | null): number {
    const parsed = Number.parseInt(value ?? "0", 10);
    return Number.isNaN(parsed) || parsed < 0 ? 0 : parsed;
}

function buildKey(term: string, gamesPage: number, usersPage: number): string {
    return `${term}|${gamesPage}|${usersPage}`;
}

function Search() {
    const [searchParams, setSearchParams] = useSearchParams();

    const query = searchParams.get("q") ?? "";
    const gamesPage = readPage(searchParams.get("gamesPage"));
    const usersPage = readPage(searchParams.get("usersPage"));

    const term = query.trim();
    const isSearchable = term.length >= MIN_TERM_LENGTH;
    const currentKey = buildKey(term, gamesPage, usersPage);

    const [state, setState] = useState<SearchState>({
        term: "",
        key: "",
        results: null,
        error: "",
    });

    useEffect(() => {
        if (!isSearchable) {
            return;
        }

        let cancelled = false;

        searchEverything(term, gamesPage, usersPage)
            .then((data) => {
                if (!cancelled) {
                    setState({ term, key: currentKey, results: data, error: "" });
                }
            })
            .catch((requestError) => {
                if (cancelled) {
                    return;
                }

                setState({
                    term,
                    key: currentKey,
                    results: null,
                    error:
                        requestError instanceof Error
                            ? requestError.message
                            : "No se pudo completar la búsqueda.",
                });
            });

        return () => {
            cancelled = true;
        };
    }, [term, gamesPage, usersPage, isSearchable, currentKey]);

    function goToPage(key: SectionKey, page: number) {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.set(key, String(page));
            return next;
        });
    }

    if (!isSearchable) {
        return (
            <main className="search-page">
                <h1>Búsqueda</h1>
                <p className="search-hint">
                    Usá la barra de búsqueda de la barra de navegación para encontrar
                    juegos y usuarios por nombre.
                </p>
            </main>
        );
    }

    const isSettled = state.key === currentKey;
    const isRefreshing = !isSettled && state.term === term;
    const isLoading = !isSettled && !isRefreshing;

    const results = isLoading ? null : state.results;
    const error = isSettled ? state.error : "";
    const isEmptyState = isSettled && error === "";

    if (isLoading) {
        return (
            <main className="search-page">
                <h1>Resultados para “{term}”</h1>
                <p className="search-status">Buscando...</p>
            </main>
        );
    }

    const totalResults = results
        ? results.games.totalItems + results.users.totalItems
        : 0;

    return (
        <main className="search-page">
            <h1>Resultados para “{term}”</h1>

            {error ? (
                <p className="search-error" role="alert">{error}</p>
            ) : (
                <p className="search-status">
                    {isRefreshing ? "Actualizando..." : `${totalResults} resultados`}
                </p>
            )}

            <section className="search-section">
                <h2>Juegos</h2>

                {error && <p className="search-empty">No se pudieron buscar los juegos.</p>}

                {isEmptyState && results && results.games.items.length === 0 && (
                    <p className="search-empty">
                        No se encontraron juegos para esta búsqueda.
                    </p>
                )}

                {!error && results && results.games.items.length > 0 && (
                    <>
                        <GameGrid games={results.games.items} />
                        <Pagination
                            label="Paginación de juegos"
                            page={results.games}
                            onChange={(page) => goToPage("gamesPage", page)}
                        />
                    </>
                )}
            </section>

            <section className="search-section">
                <h2>Foros</h2>
                <p className="search-empty">
                    La búsqueda en foros estará disponible próximamente.
                </p>
            </section>

            <section className="search-section">
                <h2>Usuarios</h2>

                {error && <p className="search-empty">No se pudieron buscar los usuarios.</p>}

                {isEmptyState && results && results.users.items.length === 0 && (
                    <p className="search-empty">
                        No se encontraron usuarios para esta búsqueda.
                    </p>
                )}

                {!error && results && results.users.items.length > 0 && (
                    <>
                        <ul className="search-users">
                            {results.users.items.map((user: SearchUser) => (
                                <li key={user.id}>
                                    <Link className="search-user" to={`/user/${user.username}`}>
                                        <span className="search-user-avatar" aria-hidden="true">
                                            {user.username.charAt(0).toUpperCase()}
                                        </span>
                                        <span>{user.username}</span>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                        <Pagination
                            label="Paginación de usuarios"
                            page={results.users}
                            onChange={(page) => goToPage("usersPage", page)}
                        />
                    </>
                )}
            </section>
        </main>
    );
}

function Pagination({ label, page, onChange }: PaginationProps) {
    if (page.totalPages <= 1) {
        return null;
    }

    return (
        <nav className="search-pagination" aria-label={label}>
            <button
                className="search-page-button"
                type="button"
                disabled={page.page === 0}
                onClick={() => onChange(page.page - 1)}
            >
                Anterior
            </button>
            <span className="search-page-status">
                Página {page.page + 1} de {page.totalPages} · {page.totalItems} resultados
            </span>
            <button
                className="search-page-button"
                type="button"
                disabled={page.page + 1 >= page.totalPages}
                onClick={() => onChange(page.page + 1)}
            >
                Siguiente
            </button>
        </nav>
    );
}

export default Search;
