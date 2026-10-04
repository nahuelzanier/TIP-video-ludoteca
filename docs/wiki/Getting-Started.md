# Getting Started

How to run Ludarium locally. The short version: you need **PostgreSQL**, **Java 21** and
**Node.js 24**, and the backend will not start without a `DB_PASSWORD`.

## Requirements

| Requirement | Version | Notes |
| --- | --- | --- |
| PostgreSQL | 14 or newer | required, not optional — see below |
| Java | 21 | `java -version` |
| Maven | via `./mvnw` | the wrapper is committed, no local install needed |
| Node.js | 18+ (CI uses 24) | |
| npm | ships with Node | |

### Why PostgreSQL is mandatory

`backend/src/main/resources/application.properties` reads:

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/ludarium}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD}
```

`DB_PASSWORD` has **no default**, so without it the application fails to start. There is no
H2, no embedded database and no in-memory fallback.

The schema is owned by **Flyway**, which runs on startup. Migration `V4__search_trigram.sql`
runs `CREATE EXTENSION IF NOT EXISTS pg_trgm`, which needs a privileged role. Either connect
once as a superuser to pre-create it:

```bash
psql -U postgres -c "CREATE EXTENSION IF NOT EXISTS pg_trgm;"
```

or use the `postgres` superuser account as `DB_USERNAME`.

There is no `docker-compose.yml` in the repository, so PostgreSQL has to be installed or
started by whatever means you already use.

## 1. Create the database

```bash
createdb -U postgres ludarium
```

Verify:

```bash
psql -U postgres -l | grep ludarium
```

You do not need to create any tables — Flyway applies V1 through V5 on the first boot.

## 2. Start the backend

From the project root:

```bash
cd backend
```

Set the password for this shell (Windows PowerShell syntax shown; adjust for your shell):

```powershell
$env:DB_PASSWORD = "your-postgres-password"
```

```bash
./mvnw spring-boot:run
```

It listens on:

```text
http://localhost:8080
```

On the first run you should see Flyway applying migrations:

```text
Migrating schema "public" to version "1 - create users"
...
Successfully applied 5 migrations
```

Verify:

```bash
curl http://localhost:8080/api/games
```

```json
{ "content": [], "page": 0, "size": 12, "totalElements": 0,
  "totalPages": 0, "hasNext": false, "hasPrevious": false }
```

An empty `content` array is correct on a fresh database: games only appear after an upload
and a publish.

### Optional environment variables

| Variable | Default | When to set it |
| --- | --- | --- |
| `DB_PASSWORD` | **none** | always |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ludarium` | different host, port or database name |
| `DB_USERNAME` | `postgres` | a non-superuser role |
| `FRONTEND_ORIGIN` | `http://localhost:5173` | if you serve the frontend from another port |

### The `dev` profile

The `dev` profile seeds a user so you can test comments and profiles without registering
first:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Credentials, from `backend/src/main/resources/application-dev.properties`:

| | |
| --- | --- |
| Username | `lucas2` |
| Email | `lucas2@gmail.com` |
| Password | `lucas222` |

The seeder is idempotent — starting the backend repeatedly does not duplicate the user. On each
run it:

1. creates `lucas2` if it does not exist,
2. reassigns every game owned by anyone else to `lucas2`, so you always have something to
   comment on, and
3. publishes every `DRAFT` game, so the catalog is never empty.

It logs a summary:

```text
Seed dev listo: usuario=lucas2 juegos reasignados=0 juegos publicados=0
```

Without the `dev` profile none of this runs, so it cannot touch a production database — though
the plaintext password is committed to the repository. See
[Known Issues](Known-Issues.md).

## 3. Start the frontend

Open a second terminal:

```bash
cd frontend
npm install
```

Create `frontend/.env.local` — it is gitignored and **required**, because `services/api.ts`
throws at load time without it:

```text
VITE_API_URL=http://localhost:8080
```

Then:

```bash
npm run dev
```

Open:

```text
http://localhost:5173
```

No dev proxy is configured, so every request goes cross-origin to `:8080`. That is why the
backend enables CORS with `allowCredentials(true)`.

## Full local startup, condensed

```bash
# terminal 1
createdb -U postgres ludarium
cd backend
export DB_PASSWORD="your-postgres-password"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# terminal 2
cd frontend
npm install
printf 'VITE_API_URL=http://localhost:8080\n' > .env.local
npm run dev
```

## Trying the features

1. **Register** at `http://localhost:5173/register` (or log in as `lucas2`).
2. **Upload a game** at `http://localhost:5173/games/upload`. The ZIP must contain
   `index.html` at its **root** — see
   [Game Upload](Game-Upload.md#preparing-a-godot-export) for a Godot export.
3. **Publish** it with the second button. It now appears on `/`.
4. **Open the game page**, play it in the iframe, and **comment** on it.
5. **Search** for it from the navbar drawer, and open your **profile**.

## Verification endpoints

```bash
# catalog (public, paginated)
curl "http://localhost:8080/api/games?page=0&size=12"

# CSRF token (public)
curl http://localhost:8080/api/auth/csrf

# current session (401 without a cookie)
curl -i http://localhost:8080/api/auth/me

# search, both sections at once (public)
curl "http://localhost:8080/api/search?q=bla&gamesPage=0&usersPage=0&size=9"

# comments for a published game (public)
curl http://localhost:8080/api/games/<gameId>/comments
```

## Running the tests

```bash
cd backend
./mvnw test
```

Eight classes, 56 tests. **Caveat:** `BackendApplicationTests` is a full `@SpringBootTest` that
needs a reachable database, so run it with `DB_PASSWORD` set and the database up — or exclude
it:

```bash
./mvnw test -Dtest='!BackendApplicationTests'
```

There are no frontend tests; CI runs `npm run lint` and `npm run build`. See
[Testing](Testing.md).

## Production build

```bash
cd frontend
npm run build      # tsc -b && vite build  ->  frontend/dist
```

```bash
cd backend
./mvnw package     # -> backend/target/backend-0.0.1-SNAPSHOT.jar
```

### There is no deployment

The repository contains **no** Dockerfile, compose file, production Spring profile or hosting
configuration. The only automation is `.github/workflows/ci.yml`, which tests and builds but
never deploys.

If you do deploy it, two things need attention:

1. **`BrowserRouter` needs a SPA fallback.** Unknown paths such as `/game/<id>` must be
   rewritten to `index.html`, otherwise deep links 404.
2. **`FRONTEND_ORIGIN` must match the real frontend origin**, because the game iframe is only
   embeddable from the origins listed in the CSP `frame-ancestors` directive.

## Troubleshooting

### The backend exits immediately with a datasource error

`DB_PASSWORD` is unset, or the database does not exist.

```bash
echo $DB_PASSWORD
psql -U postgres -l | grep ludarium
```

A symptom of the same class:

```text
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "postgres"
```

### Flyway fails on `CREATE EXTENSION pg_trgm`

Migration `V4` needs a superuser. Pre-create the extension as `postgres`, or point
`DB_USERNAME` at a role with the right privileges.

### Flyway fails with a checksum mismatch

You edited a migration that had already been applied. There is no Flyway Maven plugin
configured, so drop and recreate the database:

```bash
dropdb -U postgres ludarium
createdb -U postgres ludarium
```

### `Falta configurar VITE_API_URL en frontend/.env.local`

The file is missing or `VITE_API_URL` is not set. It must be `.env.local` (or `.env`) in
`frontend/`, and Vite only exposes variables prefixed with `VITE_`.

### The frontend cannot reach the backend

Check, in order:

1. Is the backend on `http://localhost:8080`? `curl http://localhost:8080/api/games`.
2. Does `.env.local` point at the same URL? Restart `npm run dev` after editing it — Vite
   only reads env files at startup.
3. Is `FRONTEND_ORIGIN` correct? It defaults to `http://localhost:5173`. If your frontend runs
   on another port, set it, because `allowCredentials(true)` forbids a wildcard origin.
4. Browser console, for the exact CORS failure.

### A game does not load, or the iframe is blank

- Check the response directly: `curl http://localhost:8080/games/<gameId>/index.html`. A
  `404` means the row is missing; `400` means the path failed validation.
- Confirm the game is `PUBLISHED`. Drafts are invisible to every endpoint except the asset
  routes, so the player will 404.
- Check the browser console for a CSP violation from `frame-ancestors`.

### A deep link to an older game shows "Juego no encontrado"

`Game.tsx` on this branch scans only the first 24 games of the catalog, so anything older is
unreachable. Fixed on `feature/tags`. See
[Known Issues](Known-Issues.md#deep-links-to-most-games-are-broken).

### `401` on every write

The session expired (30 minutes) or the CSRF token is stale. The frontend re-fetches
`GET /api/auth/csrf` before each write, so a hard refresh or a fresh login normally fixes it.

### The upload fails with "The ZIP must contain index.html at its root"

The ZIP contains a top-level folder, e.g. `Blasteroids/index.html`, instead of the files at
the top level. Select the contents of the export folder, not the folder itself.

### Port 8080 or 5173 already in use

Change one side and tell the other about it:

```bash
# backend
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# frontend, .env.local
VITE_API_URL=http://localhost:8081

# and, so the iframe is allowed
FRONTEND_ORIGIN=http://localhost:5173 ./mvnw spring-boot:run ...
```

## Related

- [Architecture](Architecture.md) — what the pieces are
- [Database](Database.md) — the migrations
- [Game Upload](Game-Upload.md) — preparing a game ZIP
- [Testing](Testing.md) — the test suite and CI
- [Known Issues](Known-Issues.md) — what else is broken