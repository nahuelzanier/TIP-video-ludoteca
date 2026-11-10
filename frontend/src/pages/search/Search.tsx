import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import GameGrid from "../../components/games/GameGrid";
import GameFilters from "../../components/games/GameFilters";
import { getGamesPage } from "../../services/gameService";
import { searchEverything } from "../../services/searchService";
import type { GameFilters as GameFiltersState } from "../../types/GameFilters";
import type { PageResult, SearchResponse, SearchUser } from "../../types/Search";
import "./Search.css";

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

function readTagIds(values: string[]): number[] {
    return [...new Set(values.map(Number).filter((id) => Number.isInteger(id) && id > 0))];
}

function readMinimumRating(value: string | null): number | null {
    if (value === null) return null;
    const rating = Number(value);
    return Number.isFinite(rating) && rating >= 1 && rating <= 5 ? rating : null;
}

function buildKey(
    term: string,
    gamesPage: number,
    usersPage: number,
    tagIds: number[],
    minRating: number | null,
): string {
    return [term, gamesPage, usersPage, tagIds.join(","), minRating ?? ""].join("|");
}

function Search() {
    const [searchParams, setSearchParams] = useSearchParams();

    const query = searchParams.get("q") ?? "";
    const gamesPage = readPage(searchParams.get("gamesPage"));
    const usersPage = readPage(searchParams.get("usersPage"));
    const selectedTagIds = readTagIds(searchParams.getAll("tagIds"));
    const minRating = readMinimumRating(searchParams.get("minRating"));

    const term = query.trim();
    const isSearchable = term.length >= MIN_TERM_LENGTH;
    const hasActiveFilters = selectedTagIds.length > 0 || minRating !== null;
    const canLoadResults = isSearchable || hasActiveFilters;
    const currentKey = buildKey(term, gamesPage, usersPage, selectedTagIds, minRating);
    const selectedTagsKey = selectedTagIds.join(",");

    const [state, setState] = useState<SearchState>({
        term: "",
        key: "",
        results: null,
        error: "",
    });

    useEffect(() => {
        if (!canLoadResults) {
            return;
        }

        let cancelled = false;
        const requestTagIds = selectedTagsKey === ""
            ? []
            : selectedTagsKey.split(",").map(Number);
        const filters: GameFiltersState = { tagIds: requestTagIds, minRating };

        const request = isSearchable
            ? searchEverything(term, gamesPage, usersPage, 9, filters)
            : getGamesPage(gamesPage, 9, "", filters).then((page) => ({
                query: "",
                games: {
                    items: page.content,
                    page: page.page,
                    size: page.size,
                    totalItems: page.totalElements,
                    totalPages: page.totalPages,
                },
                users: {
                    items: [],
                    page: 0,
                    size: 9,
                    totalItems: 0,
                    totalPages: 0,
                },
            }));

        request
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
                            : "Could not complete the search.",
                });
            });

        return () => {
            cancelled = true;
        };
    }, [
        term,
        gamesPage,
        usersPage,
        minRating,
        selectedTagsKey,
        canLoadResults,
        isSearchable,
        currentKey,
    ]);

    function goToPage(key: SectionKey, page: number) {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.set(key, String(page));
            return next;
        });
    }

    function updateFilters(filters: GameFiltersState) {
        setSearchParams((current) => {
            const next = new URLSearchParams(current);
            next.delete("tagIds");
            filters.tagIds.forEach((tagId) => next.append("tagIds", String(tagId)));

            if (filters.minRating === null) {
                next.delete("minRating");
            } else {
                next.set("minRating", String(filters.minRating));
            }

            next.set("gamesPage", "0");
            return next;
        });
    }

    if (!canLoadResults) {
        return (
            <main className="search-page">
                <h1>Search</h1>
                <p className="search-hint">
                    Search for games or creators, or choose a category from the home page.
                </p>
            </main>
        );
    }

    const isSettled = state.key === currentKey;
    const isRefreshing = !isSettled && state.key !== "" && state.term === term;
    const isLoading = !isSettled && !isRefreshing;

    const results = isLoading ? null : state.results;
    const error = isSettled ? state.error : "";
    const isEmptyState = isSettled && error === "";

    if (isLoading) {
        return (
            <main className="search-page">
                <h1>{term ? `Results for “${term}”` : "Games by category"}</h1>
                <p className="search-status">Loading games...</p>
            </main>
        );
    }

    const totalResults = results
        ? results.games.totalItems + results.users.totalItems
        : 0;

    return (
        <main className="search-page">
                <h1>{term ? `Results for “${term}”` : "Games by category"}</h1>

            {error ? (
                <p className="search-error" role="alert">{error}</p>
            ) : (
                <p className="search-status">
                    {isRefreshing ? "Updating..." : `${totalResults} results`}
                </p>
            )}

            <section className="search-section">
                <h2>Games</h2>
                <GameFilters
                    filters={{ tagIds: selectedTagIds, minRating }}
                    onChange={updateFilters}
                />

                {error && <p className="search-empty">Could not search games.</p>}

                {isEmptyState && results && results.games.items.length === 0 && (
                    <p className="search-empty">
                        No games found for this search.
                    </p>
                )}

                {!error && results && results.games.items.length > 0 && (
                    <>
                        <GameGrid games={results.games.items} />
                        <Pagination
                            label="Game pages"
                            page={results.games}
                            onChange={(page) => goToPage("gamesPage", page)}
                        />
                    </>
                )}
            </section>

            {isSearchable && (
              <>
                <section className="search-section">
                    <h2>Forums</h2>
                    <p className="search-empty">
                        Forum search will be available soon.
                    </p>
                </section>
                <section className="search-section">
                <h2>Users</h2>

                {error && <p className="search-empty">Could not search users.</p>}

                {isEmptyState && results && results.users.items.length === 0 && (
                    <p className="search-empty">
                        No users found for this search.
                    </p>
                )}

                {!error && results && results.users.items.length > 0 && (
                    <>
                        <ul className="search-users">
                            {results.users.items.map((user: SearchUser) => (
                                <li key={user.id}>
                                    <Link className="search-user" to={`/user/${user.username}`}>
                                        <SearchUserAvatar user={user} />
                                        <span>{user.username}</span>
                                    </Link>
                                </li>
                            ))}
                        </ul>
                        <Pagination
                            label="User pages"
                            page={results.users}
                            onChange={(page) => goToPage("usersPage", page)}
                        />
                    </>
                )}
                </section>
              </>
            )}
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
                Previous
            </button>
            <span className="search-page-status">
                Page {page.page + 1} of {page.totalPages} · {page.totalItems} results
            </span>
            <button
                className="search-page-button"
                type="button"
                disabled={page.page + 1 >= page.totalPages}
                onClick={() => onChange(page.page + 1)}
            >
                Next
            </button>
        </nav>
    );
}

function SearchUserAvatar({ user }: { user: SearchUser }) {
    const [failedUrl, setFailedUrl] = useState<string | null>(null);

    const showImage = Boolean(user.avatarUrl) && failedUrl !== user.avatarUrl;

    return (
        <span className="search-user-avatar" aria-hidden="true">
            {showImage ? (
                <img
                    src={user.avatarUrl ?? undefined}
                    alt=""
                    loading="lazy"
                    onError={() => setFailedUrl(user.avatarUrl)}
                />
            ) : (
                user.username.charAt(0).toUpperCase()
            )}
        </span>
    );
}

export default Search;
