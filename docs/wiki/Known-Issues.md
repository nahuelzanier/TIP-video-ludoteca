# Known Issues & Technical Debt

An honest inventory of what is broken, missing or risky in the current state of the
repository. Nothing here is a design proposal; every item is something observable in the code.

Most items are on the working branch `featureComentarios`. The last section lists what
`feature/tags` already fixes.

## Branch divergence

| Fact | Detail |
| --- | --- |
| `main` is at PR #1 | `f6114d1`, 2026-09-24 — "Merge pull request #1 ... login_users" |
| The working branch is far ahead | `featureComentarios` is 24 commits on top of `main` |
| `main` is a strict ancestor | it has nothing the other branches lack — it is simply stale |
| `dev` | `featureComentarios` minus the last commit (`ff44720`, the comments) |
| `origin/feature/tags` | 10 more commits, adds tags + fixes some of these issues |
| CI only runs on `main` pushes and PRs | so none of the 24 feature commits were validated by CI |

Anything reviewing or deploying from `main` sees a broken app: `main` still has the
`games.json` catalog and the `backend/gamesData/` folders, but its `GameApiController` and
`SearchController` expect a database.

### Only on `main` (deleted everywhere else)

`git diff --stat main featureComentarios` shows 136 files changed. The `-` side is content
that only `main` has:

- `backend/gamesData/**` — 31 files, **~142.6 MB**, mostly Godot `.wasm` and `.pck` bundles.
  Three `.wasm` files are 36 MB, 36 MB and 17 MB, stored raw in git history with no Git LFS.
- `backend/src/main/resources/games.json` — the old catalog, removed in commit `5642513`.
- `frontend/public/images/game1.png`, `game2.png`, `game3.png`.

These deletions have not been merged forward, so `main` still carries 142 MB of binaries that
the current code cannot serve.

## Frontend bugs

### Deep links to most games are broken

`pages/Game.tsx` resolves the game by scanning the catalog client-side:

```tsx
getGames().then((games) => {
  const foundGame = games.find((game) => game.id === id);
  setGame(foundGame ?? null);
});
```

and `getGames()` is:

```ts
export async function getGames(): Promise<Game[]> {
  const result = await getGamesPage(0, 24);
  return result.content;
}
```

So a game is only reachable if it happens to be among the **24 newest**. Anything older, or on
a later page, renders `"Juego no encontrado"` even though the URL is correct.

Two further problems in the same file:

- `game.id` is interpolated into the iframe URL **without** `encodeURIComponent`, unlike every
  service call.
- There is **no `.catch()`** on the request. A failed fetch leaves an unhandled rejection and
  the page stuck on its null state.
- `"Juego no encontrado"` doubles as the loading state, so there is no loading indicator.

Fixed on `feature/tags` by the new `GET /api/games/{gameId}` endpoint and `getGameById()`.

### No 404 route

`App.tsx` has no `*` element. An unknown URL renders an empty `<Outlet />` under the navbar —
a blank page with a working menu, which reads as a rendering bug rather than a missing page.

### The navbar shows "Log out" to anonymous visitors

`components/navbar/navbar.tsx` never checks the session. The button is always visible. The
call is harmless (the server returns `204` either way) but it is misleading.

### Pagination on the home page is not in the URL

`Home.tsx` keeps the page number in `useState`. Reloading or sharing `/` always shows page 0,
and there is no back/forward support. The search page does it correctly with
`useSearchParams`, so there are two separate pagination implementations with different
behaviour.

### No random game

`pages/randomgame/RandomGame.tsx` renders a single `<h1>` and contains no selection logic at
all. The navbar button labelled "Randomize game" just navigates there.

### A forum link points at a route that does not exist

`components/profile/UserActivity.tsx` renders:

```tsx
<Link to={`/forum/${forum.id}`}>{forum.title}</Link>
```

There is no `/forum/:id` route — only `/forum`. The list is empty in practice because
`pages/user/user.tsx` hard-codes `const FORUMS: ForumSummary[] = [];`, so the component never
renders a link today. It is a latent break.

### Settings and Forum are placeholders

Both render a single `<h1>`. There is no settings persistence and no forum backend at all.

## Backend issues

### Error bodies are inconsistent

`GlobalExceptionHandler` produces `{"error": "..."}`, but several code paths throw
`ResponseStatusException`, which Spring renders as a `ProblemDetail`:

| Source | Body shape |
| --- | --- |
| `GlobalExceptionHandler` | `{"error":"El juego no existe."}` |
| `GameUploadService` (all validation) | `{"status":400,"error":"Bad Request","detail":"..."}` |
| `GameTagService` (on `feature/tags`) | same as above |
| `MaxUploadSizeExceededException` | Spring's default, not JSON at all |
| `InvalidMediaTypeException` | Spring's default |
| `authenticationEntryPoint` (`401`) | empty body |

`services/gameUploadService.ts` parses `detail` before `error` to compensate, and
`services/searchService.ts` has a duplicated `readError` that still only reads `error`. The
clean fix is to add a `@ExceptionHandler(ResponseStatusException.class)` to
`GlobalExceptionHandler`.

### Upload reads the whole archive into memory

`GameUploadService.readArchive` buffers every entry in a `ByteArrayOutputStream` and the
method then calls `gameFiles.saveAll(...)` with all of them held in one list. A 100 MB ZIP
therefore needs roughly 100 MB of heap **plus** the driver buffers, in a single transaction.
There is no streaming path and no `spring.jpa.properties.hibernate.jdbc.batch_size` tuning.

### CORS is declared twice, one place hard-coded

`config/WebConfig.java` configures CORS globally from `app.frontend-origin`, but
`GameApiController` and `GameCoverController` also carry:

```java
@CrossOrigin(origins = "http://localhost:5173")
```

`@CrossOrigin` takes precedence for those routes, so pointing `FRONTEND_ORIGIN` at a
different origin does **not** work for the catalog and the covers. Removing both annotations
would make `FRONTEND_ORIGIN` authoritative everywhere.

### A dev password is committed in plaintext

`backend/src/main/resources/application-dev.properties` is tracked:

```properties
app.seed.username=lucas2
app.seed.email=lucas2@gmail.com
app.seed.password=lucas222
```

It is only used under `@Profile("dev")`, so it never reaches a production database. It is
still a real credential in git history, and it is `application-dev.properties` rather than an
ignored `.env` file. Moving it to an environment variable would be a small change.

### Controllers bypass the service layer

`GameApiController` and `SearchController` inject repositories directly:

```java
private final GameRepository games;
```

Every other feature goes through a service (`CommentService`, `GameService`,
`GameUploadService`, `UserProfileService`). This makes those two controllers harder to test and
means their clamping and sorting rules are not unit-testable without a Spring context.

### Two different page DTO shapes

`GameApiController.GamePage` and `SearchController.PageResult<T>` describe the same concept
with different field names:

| Endpoint | Total field | Navigation flags |
| --- | --- | --- |
| `GET /api/games` | `totalElements` | `hasNext`, `hasPrevious` |
| `GET /api/search` | `totalItems` | none |

The frontend has to handle both, with two separate pagination components as a result.

### CORS config has broken indentation

`config/WebConfig.java` lines 15-16 are indented as if they were still inside the class
declaration:

```java
        public WebConfig(@Value("${app.frontend-origin}") String frontendOrigin) {
            this.frontendOrigin = frontendOrigin;
```

Cosmetic, but it is inconsistent with the rest of the file.

### `WebConfig` has two blank lines after the import block

Minor formatting noise in the same file.

## Security notes

Not bugs, but worth stating explicitly:

- **The iframe is not sandboxed.** `GameSection.tsx` renders `<iframe src={gameUrl}>` with no
  `sandbox`, `allow` or `referrerPolicy`. An uploaded game runs with the same origin as the
  backend and can call any public API endpoint. The cover is protected by magic-byte validation
  and `nosniff`, but game HTML and JS are served as-is.
- **`X-Content-Type-Options: nosniff` is set** on both `GameController` and
  `GameCoverController`, which is good.
- **No rate limiting** on register, login, comment creation or upload.
- **No moderation.** Comments and profiles can be deleted only by direct database access.
- **No file-size limit on comments pagination** — a game with thousands of comments returns all
  of them in one response.

## Tooling issues

### `tsconfig.app.json` does not enable `strict`

```jsonc
{
  "compilerOptions": {
    "target": "es2023",
    // ...
    "noUnusedLocals": true,
    "noUnusedParameters": true
    // no "strict": true
  }
}
```

`tsc -b` runs in CI, but without `strict` a lot of `null` and `undefined` handling goes
unchecked. Turning it on would surface real errors and is the single highest-value frontend
cleanup.

### A git case collision between `Navbar/` and `navbar/`

Git tracks **three** files for one component:

```text
frontend/src/components/Navbar/navbar.tsx     <- 7129 bytes
frontend/src/components/navbar/Navbar.css    <- 4436 bytes
frontend/src/components/navbar/Navbar.tsx    <- 7129 bytes
```

On Windows/NTFS the two `.tsx` paths collapse into one physical file, so `git status` stays
clean and the build works. On a case-sensitive filesystem (Linux CI, Docker) checkout creates
**both** `.tsx` files. Today they are byte-identical, so the build still succeeds — but there
is one component living at two paths, and `App.tsx` imports
`./components/navbar/navbar`, which is neither of the canonical casings on the tags branch.

`feature/tags` resolves this by renaming to `components/navbar/Navbar.tsx`.

### CI has no database

`BackendApplicationTests` is a plain `@SpringBootTest`:

```java
@SpringBootTest
class BackendApplicationTests {
    @Test
    void contextLoads() { }
}
```

The `backend` CI job provisions **no PostgreSQL service** and sets no `DB_PASSWORD`, while
`application.properties` requires it. This test needs a reachable database, so it either fails
in CI or depends on whatever happens to be listening on `localhost:5432`. There is no
`application-test.properties`, no H2 and no Testcontainers.

### Dead files

| Path | Size | Problem |
| --- | --- | --- |
| `frontend/src/App.css` | 3 KB | 184 lines of Vite template styles (`.hero`, `#center`, `#next-steps`), imported by nothing |
| `frontend/public/icons.svg` | 5 KB | social-icon sprite, zero references in `src/` |
| `frontend/src/assets/hero.png` | 13 KB | template asset, zero imports |
| `frontend/src/assets/react.svg` | 4 KB | template asset, zero imports |
| `frontend/src/assets/vite.svg` | 8 KB | template asset, zero imports |
| `frontend/README.md` | — | still the stock "React + TypeScript + Vite" template readme |
| `mock/LudariumMU.png` | **1.5 MB** | a design mockup referenced by nothing in the repo |
| `package-lock.json` (root) | 103 B | an empty `{"packages": {}}` lockfile with **no root `package.json`**; exists only to make Dependabot find `frontend/package-lock.json` |

### `--accent-border` is referenced but never defined

`Navbar.css` and `App.css` use `var(--accent-border, var(--border))`, but `index.css` defines
no `--accent-border`. The fallback saves it, but the token is missing from the design system.

### A Vite README inside the frontend

`frontend/README.md` describes the template, not Ludarium. Everything the wiki says about the
frontend contradicts it.

### There is no deployment configuration

No `Dockerfile`, no `docker-compose.yml`, no `Procfile`, no `render.yaml`, no `nginx.conf`, no
`application-prod.properties`. `application-prod.properties` does not exist, and there is no
profile other than `dev`. The only automation in the repository is `.github/workflows/ci.yml`,
which builds and tests but never deploys.

### No root `.gitignore`

Only `backend/.gitignore` and `frontend/.gitignore` exist. They do cover `backend/target/`
and `frontend/node_modules/` + `dist/`, so this works today, but a new top-level build output
would be tracked by accident.

### No root `.gitattributes`

`backend/.gitattributes` only fixes line endings for `mvnw` and `*.cmd`. Nothing marks
`*.png`, `*.wasm`, `*.pck` or `*.jar` as binary, so git auto-detects. With 36 MB `.wasm`
blobs in `main`'s history, an explicit binary declaration and Git LFS would be worth it.

## Already fixed on `feature/tags`

Listed so nobody re-investigates them:

| Issue | Fix |
| --- | --- |
| Broken deep links in `Game.tsx` | new `GET /api/games/{gameId}` + `getGameById()` |
| `game.id` not encoded in the iframe URL | `encodeURIComponent(game.id)` |
| Missing loading and error states in `Game.tsx` | explicit `loading` state and `.catch()` |
| No "games by this user" listing | new `GET /api/users/{username}/games` |
| The `Navbar/` vs `navbar/` collision | renames to `Navbar.tsx`, `Search.tsx`, `User.tsx` and their CSS files |
| CSRF prefetch duplicated five times | not yet fixed — still worth doing |

## Prioritising

If time is limited, this order gives the most value per unit of work:

1. Merge `dev` → `main` so CI and reviewers see the real application.
2. Merge `feature/tags`, which fixes the game page and the case collision.
3. Enable `"strict": true` in `tsconfig.app.json` and fix what surfaces.
4. Add a `@ExceptionHandler(ResponseStatusException.class)` so error bodies are uniform.
5. Remove the two `@CrossOrigin` annotations so `FRONTEND_ORIGIN` works everywhere.
6. Add a PostgreSQL service container to the CI backend job.
7. Cover `GameUploadService` with tests — it is the only untested code that handles untrusted
   binary input.
8. Add a `*` route and a 404 page.
9. Delete the dead files listed above, including `mock/LudariumMU.png`.

## Related

- [Testing](Testing.md) — the current suite and its gaps
- [Architecture](Architecture.md) — where these files sit
- [Tags](Tags.md) — the unmerged feature branch