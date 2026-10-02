# Frontend

The frontend is a single-page React app. It has **three runtime dependencies** and nothing
else: no state library, no component library, no HTTP client, no test runner.

## Stack

| Package | Version |
| --- | --- |
| `react` / `react-dom` | `^19.2.8` |
| `react-router-dom` | `^7.18.3` |
| `vite` | `^8.2.2` |
| `typescript` | `~6.0.2` |

Dev-only: `@vitejs/plugin-react`, `eslint` + `typescript-eslint` + `eslint-plugin-react-hooks`
+ `eslint-plugin-react-refresh`, `@types/*`, `globals`.

All HTTP goes through the **native `fetch` API**. There is no `axios`.

## Scripts

| Script | Command |
| --- | --- |
| `npm run dev` | `vite` — dev server on `http://localhost:5173` |
| `npm run build` | `tsc -b && vite build` |
| `npm run lint` | `eslint .` |
| `npm run preview` | `vite preview` — serves the built `dist/` |

## Configuration

### `VITE_API_URL` is mandatory

`services/api.ts` throws at module load if it is missing:

```ts
const baseUrl = import.meta.env.VITE_API_URL;
if (!baseUrl) {
  throw new Error("Falta configurar VITE_API_URL en frontend/.env.local");
}

export const API_BASE_URL = baseUrl.replace(/\/$/, "");
```

Create `frontend/.env.local` (gitignored) with:

```text
VITE_API_URL=http://localhost:8080
```

It is the **only** environment variable the app reads. There is no `.env.example` and no
`ImportMetaEnv` augmentation; typing comes from `"types": ["vite/client"]` in
`tsconfig.app.json`.

### `vite.config.ts`

Seven lines, no customisation:

```ts
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],
})
```

Notably there is **no `server.proxy`**. The frontend talks to the backend cross-origin, which
is why the backend needs CORS with `allowCredentials(true)` — see
[Authentication](Authentication.md#cors).

There are no path aliases either; every import is relative.

## Routing

`src/App.tsx` declares one pathless layout route that renders the navbar above every page:

```tsx
function MainLayout() {
  return (
    <>
      <Navbar />
      <Outlet />
    </>
  );
}
```

| Path | Component | File |
| --- | --- | --- |
| `/` | `Home` | `pages/Home.tsx` |
| `/game/:id` | `Game` | `pages/Game.tsx` |
| `/login` | `AuthPage mode="login"` | `pages/AuthPage.tsx` |
| `/register` | `AuthPage mode="register"` | `pages/AuthPage.tsx` |
| `/profile` | `Profile` | `pages/profile/Profile.tsx` |
| `/settings` | `Settings` | `pages/settings/Settings.tsx` |
| `/randomgame` | `RandomGame` | `pages/randomgame/RandomGame.tsx` |
| `/forum` | `Forum` | `pages/forum/Forum.tsx` |
| `/search` | `Search` | `pages/search/search.tsx` |
| `/user/:username` | `User` | `pages/user/user.tsx` |
| `/games/upload` | `UploadGame` | `pages/uploadgame/UploadGame.tsx` |

Three things to know about this table:

- **There is no `*` catch-all route.** An unknown URL renders an empty `<Outlet />` under the
  navbar.
- **There are no route guards.** Pages redirect themselves using `<Navigate>` after calling
  `getCurrentUser()`. `/profile` and `/games/upload` do this, and `/profile` then bounces to
  `/user/{username}`.
- **`BrowserRouter` needs a SPA fallback.** Deep links such as `/game/<id>` only work in
  development; a production server must rewrite unknown paths to `index.html`.

### File naming is inconsistent

Some page folders use PascalCase files, others do not:

| PascalCase | lowercase |
| --- | --- |
| `pages/profile/Profile.tsx` | `pages/search/search.tsx` |
| `pages/settings/Settings.tsx` | `pages/user/user.tsx` |
| `pages/forum/Forum.tsx` | `components/navbar/navbar.tsx` |
| `pages/randomgame/RandomGame.tsx` | `pages/AuthPage.tsx`, `pages/Home.tsx`, `pages/Game.tsx` |

The `feature/tags` branch renames the lowercase ones. It also cleans up a git case collision:
`App.tsx` currently imports `./components/navbar/navbar`, while git tracks the component at
both `components/Navbar/navbar.tsx` and `components/navbar/Navbar.tsx`. See
[Known Issues](Known-Issues.md).

## Pages

### `Home` (`/`)

Paginated catalog grid.

- Loads `getGamesPage(page)` with the default `size = 12`.
- Renders `"Games"`, a `{totalElements} game(s)` counter, a loading line, an error with
  `role="alert"`, or `"No published games yet."` when empty.
- `GameGrid` renders the cards.
- Pagination is a `<nav aria-label="Game pages">` shown only when `totalPages > 1`, with
  `Previous` / `Page {n} of {total}` / `Next`.
- **The page number is component state, not a URL parameter.** A reload goes back to page 0.

### `Game` (`/game/:id`)

- Resolves the game with `useParams`, then finds it in `getGames()`.
- Shows the title, the description, the player, and the comment section.
- The iframe URL is built here:

```tsx
const gameUrl = `${API_BASE_URL}/games/${game.id}/index.html`;
```

> On this branch `game.id` is **not** passed through `encodeURIComponent`, unlike every
> service call. It also resolves the game by scanning the first 24 games of the catalog, so
> deep-linking to a newer or rarer game shows `"Juego no encontrado"`. Both are fixed on
> `feature/tags` with the new `GET /api/games/{id}` endpoint. See
> [Known Issues](Known-Issues.md#deep-links-to-most-games-are-broken).

### `AuthPage` (`/login`, `/register`)

One component, two modes via a `mode` prop.

- Registration constraints mirror the server: username `minLength={3} maxLength={30}
  pattern="[A-Za-z0-9_]+"`, password `minLength={8} maxLength={72}`, `type="email"`.
- Client-side check `if (isRegister && password !== confirmPassword)`.
- On register it calls `register(...)` and then **unconditionally** `login(...)`, so a new
  account lands already logged in.
- On success it navigates to `location.state.from ?? "/profile"`, replacing the history entry,
  so the profile page's redirect chain lands on the public profile.
- Renders `<Link className="auth-back" to="/">← Volver a la ludoteca</Link>` and a
  cross-link between login and register.

### `Profile` (`/profile`)

A pure redirect shim. It resolves `getCurrentUser()` and then:

```tsx
if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
return <Navigate to={`/user/${user.username}`} replace />;
```

The real profile UI lives at `/user/:username`.

### `User` (`/user/:username`)

The public profile.

- Loads `getUserProfile(username)` and `getCurrentUser()` in two separate effects.
- **Ownership is decided by comparing ids**: `const isOwnProfile = profile !== null &&
  currentUser?.id === profile.id;`.
- Renders `ProfileHeader`, `UserActivity`, and the description modal.
- Distinct states for loading, load error (with a link to `/search`), and user-not-found.
- `const FORUMS: ForumSummary[] = [];` is a hard-coded empty array, so `UserActivity` always
  shows its empty state. There is no forum API yet.

### `Search` (`/search`)

Fully URL-driven with `useSearchParams` — `q`, `gamesPage`, `usersPage`.

- Terms shorter than 2 characters never hit the network; the page shows
  `"Usá la barra de búsqueda de la barra de navegación para encontrar juegos y usuarios por
  nombre."`, mirroring the server's `MIN_TERM_LENGTH`.
- Loads both sections with one `searchEverything(term, gamesPage, usersPage)` call.
- Tracks a `buildKey(term, gamesPage, usersPage)` string to distinguish
  "refreshing" (`"Actualizando..."`) from a cold `"Buscando..."`.
- Three sections: games (a `GameGrid` with its own pagination), forums (a hard-coded
  `"La búsqueda en foros estará disponible próximamente."`), and users (a list of links with
  the first letter as the avatar, with its own pagination).
- The `Pagination` helper returns `null` when `totalPages <= 1`.

### `UploadGame` (`/games/upload`)

See [Game Upload](Game-Upload.md#the-frontend-flow). Guards on the session, validates the
cover client-side, uploads as a draft, then offers a second button to publish.

### Placeholder pages

`Forum`, `Settings` and `RandomGame` render a single `<h1>` and nothing else. `RandomGame` does
**not** pick a random game — the navbar button simply navigates to `/randomgame`.

## Components

```text
components/
  comments/
    CommentSection.tsx     list + empty/loading/error states, picks form vs "log in" notice
    CommentForm.tsx        auto-expanding textarea, live 1000-char counter, star rating
    CommentItem.tsx        one comment, reply toggle, inline reply form
    ReplyList.tsx          null when there are no replies, otherwise maps to CommentItem
    StarRating.tsx         read-only (span) or interactive (buttons) — decided by onChange
    UserBadge.tsx          avatar, username link, "(autor)" badge, stars, es-AR date
  games/
    GameCard.tsx           <Link> with a 16/9 cover and title/description
    GameGrid.tsx           3 / 2 / 1 column CSS grid
    GameSection.tsx        the <iframe> player
    GameTagSection.tsx     only on feature/tags
  navbar/
    navbar.tsx             hamburger + off-canvas drawer
  profile/
    ProfileHeader.tsx      avatar, name, description, owner-only actions
    EditDescriptionModal.tsx  dialog with Escape-to-close and a live counter
    UserActivity.tsx       empty-state only, see above
```

### `GameSection` — the embed

```tsx
<section className="game-section">
  <iframe src={gameUrl} title="Juego" className="game-frame" />
</section>
```

CSS forces `width: 100%`, `aspect-ratio: 16/9` and `border: none`. There is no `sandbox`,
`allow`, `allowFullScreen` or `loading` attribute. The backend cooperates via
`frameOptions(...disable())` and a CSP of `frame-ancestors 'self' <frontendOrigin>`.

### `Navbar` — an off-canvas drawer

Not a top bar. A fixed hamburger button (`aria-label="Open menu"`) in the top-right opens an
`aside.navbar-drawer` with an overlay that closes on click, and a `keydown` listener that
closes it on Escape.

Contents, top to bottom:

1. Brand text `Ludoteca`, plus a close button.
2. A search form (`role="search"`) that navigates to `/search?q=...` and ignores terms under 2
   characters.
3. `Main menu`: Home, Profile, Settings — `NavLink`s with an `--active` modifier.
4. `Actions`: a `Randomize game` button and a Forum link.
5. A logout button.

Active styling uses the render-prop form:

```tsx
<NavLink to="/" end className={({ isActive }) =>
  `navbar-link ${isActive ? "navbar-link--active" : ""}`}>
```

The drawer shows "Log out" even to anonymous visitors, because it never checks the session.

## Services

`src/services/` is the only place that calls `fetch`.

| File | Exports | Endpoints |
| --- | --- | --- |
| `api.ts` | `API_BASE_URL`, `fetchCsrfToken`, `readError` | `GET /api/auth/csrf` |
| `authService.ts` | `register`, `login`, `getCurrentUser`, `logout` | register, login, me, csrf, logout |
| `commentService.ts` | `getGameComments`, `createComment`, `createReply` | 3 comment endpoints |
| `gameService.ts` | `getGamesPage`, `getGames` | `GET /api/games` |
| `gameUploadService.ts` | `uploadGame`, `publishGame` | `POST /api/games`, publish |
| `searchService.ts` | `searchEverything` | `GET /api/search` |
| `userService.ts` | `getUserProfile`, `updateDescription` | profile GET/PATCH |

### Error handling

`readError` in `api.ts` is the shared parser:

```ts
export async function readError(response: Response): Promise<string> {
  const text = await response.text();
  try {
    const body = JSON.parse(text) as { error?: string };
    return body.error || text || `Error ${response.status}`;
  } catch {
    return text || `Error ${response.status}`;
  }
}
```

It reads `{"error": "..."}`, which is what `GlobalExceptionHandler` produces. Two services
work around the exceptions:

- `searchService.ts` has a **local duplicate** of `readError` instead of importing the shared
  one.
- `gameUploadService.ts` parses `detail` **before** `error`, because upload errors arrive as
  Spring `ProblemDetail` bodies.

### Relative cover URLs

The catalog stores the cover as `/api/games/{id}/cover`. Both `gameService` and
`searchService` resolve it against the API base:

```ts
image: new URL(game.image, API_BASE_URL).toString()
```

This is why the cover never has to be proxied or hard-coded per environment.

### Session handling per request

- `credentials: "include"` on every session-dependent call.
- `getCurrentUser()` returns `null` on `401` instead of throwing.
- `getUserProfile()` returns `null` on `404`, which is what triggers the "Usuario no
  encontrado" view.
- Writes that receive `401` throw `"Tu sesión expiró. Volvé a iniciar sesión."` without
  navigating anywhere.
- Multipart uploads must not set `Content-Type`; the browser adds the boundary.

See [Authentication](Authentication.md#how-the-frontend-handles-the-session).

## Types

```text
types/
  Game.ts      Game { id, title, image, description }
  Comment.ts   Comment { id, content, rating, createdAt, username,
                         profileImageUrl, isGameAuthor, replies }
               CreateCommentPayload { content, rating? }
  Search.ts    SearchUser, PageResult<T>, GamePage, UserPage, SearchResponse
  User.ts      UserProfile { id, username, description }, ForumSummary
```

There is no `vite-env.d.ts`. The `CsrfResponse` shape and several prop interfaces are
declared inside the services and components that own them rather than in `types/`.

## State management

**None.** There is no Context, no Redux, no Zustand and no React Query anywhere — zero
occurrences of `createContext`, `useContext` or `Provider` in `src/`.

Every component owns its own `useState` and fetches its own data:

- The page number on `Home` is local state.
- The search term and its two page numbers live in the URL.
- `useComments` is the **only** custom hook in the project; it holds the comment list, the
  current user, and the loading/error/submitting flags, then re-fetches the whole list after
  any successful write.
- `Profile`, `UploadGame` and `User` each call `getCurrentUser()` on mount, so navigating
  between them issues the same request again.

### `useComments`

```ts
interface UseCommentsResult {
  comments: Comment[];
  currentUser: AuthUser | null;
  loading: boolean;
  error: string;
  submitting: boolean;
  submitError: string;
  submitComment: (content: string, rating: number | null) => Promise<boolean>;
  submitReply: (commentId: number, content: string) => Promise<boolean>;
}
```

Both effects use a `cancelled` / `isCurrentRequest` cleanup flag to ignore stale responses.
After a write it re-reads the full thread rather than optimistically patching the list, which
is why the rating and the reply nesting always match what the server computed.

## Styling

Hand-written global CSS with a BEM-ish convention. One `.css` file per component, imported
from the TSX with `import "./X.css";`.

- No CSS Modules, no CSS-in-JS, no Tailwind, no Sass, no PostCSS.
- `src/index.css` is imported once from `main.tsx` and defines every design token on `:root`:

```css
--text: #4b4554;         --text-h: #1f1b26;
--bg: #f6f6f8;           --surface: #ffffff;
--border: #e5e4e7;
--accent: #863bff;       --accent-hover: #6f1ff0;
--accent-bg: rgba(134, 59, 255, 0.08);
--shadow-sm: 0 1px 2px rgba(31,27,38,0.06);
--shadow-md: 0 4px 12px rgba(31,27,38,0.08);
--shadow-lg: 0 12px 28px rgba(31,27,38,0.12);
--sans: system-ui, 'Segoe UI', Roboto, sans-serif;
--heading: system-ui, 'Segoe UI', Roboto, sans-serif;
```

- **Light theme only.** There is no dark mode and no `prefers-color-scheme` block.
- Pages share a shell of `max-width: 1200px; margin: 0 auto; padding: 48px 24px 64px;` plus an
  `h1::after` gradient underline.
- Focus is consistent everywhere: `outline: 2px solid var(--accent); outline-offset: 2px;`.
- Error text is `#b42318`; success text `#16713b`.
- `pages/uploadgame/UploadGame.css` is the only file with hard-coded hex colours, bypassing
  the variables.
- Breakpoints in use: `1200px`, `900px`, `600px`, `480px`.

## Linting

`eslint.config.js` extends `js.configs.recommended`, `tseslint.configs.recommended` (the
**non** type-checked variant), `reactHooks.configs.flat.recommended` and
`reactRefresh.configs.vite`, with only `dist` ignored.

`reactHooks` matters most here: almost every page sets state from inside an effect, so
`exhaustive-deps`, `set-state-in-effect` and `refs` are the rules most likely to fire.

> `tsconfig.app.json` does not set `"strict": true`. The type checker is therefore quite
> permissive. See [Known Issues](Known-Issues.md).

## Tests

**There are none.** No `*.test.*` or `*.spec.*` files, no test runner in `package.json`, no
Vitest, Jest, Playwright or Testing Library dependency. CI only runs `npm run lint` and
`npm run build`. See [Testing](Testing.md).

## Dead files

Carried over from the Vite template, referenced by nothing:

- `src/App.css` — 184 lines of template styles (`.hero`, `#center`, `#next-steps`, ...)
- `src/assets/hero.png`, `src/assets/react.svg`, `src/assets/vite.svg`
- `public/icons.svg` — an SVG sprite for social icons, never imported
- `public/favicon.svg` — referenced by `index.html`, so this one **is** used
- `README.md` — still the stock "React + TypeScript + Vite" template readme

## Related

- [API](API.md) — the endpoints this app consumes
- [Architecture](Architecture.md) — how the two halves fit together
- [Known Issues](Known-Issues.md) — the gaps listed above, in detail