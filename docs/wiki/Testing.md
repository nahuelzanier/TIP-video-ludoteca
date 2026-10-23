# Testing & CI

## Run everything locally

Start Docker Desktop and wait until its engine is ready. Then, from the repository root:

```powershell
cd backend
.\mvnw.cmd verify
```

Testcontainers starts an isolated PostgreSQL 18 container automatically when a test needs the
database. Flyway applies the project migrations to that temporary database. The container is
stopped when the test JVM exits; tests do not connect to or modify the PostgreSQL database used
by the application. Surefire runs the test JVM in UTC so the database receives a timezone name
it supports, regardless of the developer machine's configured timezone.

No database server, database credentials, or manually started container are needed for tests.
Docker Desktop must be running locally. GitHub Actions provides Docker on its runner, so the
same command works in CI without a separate PostgreSQL service configuration.

`verify` also creates the JaCoCo report at `backend/target/site/jacoco/index.html`.

Run a single backend class or method with:

```powershell
.\mvnw.cmd test "-Dtest=CommentServiceTests"
.\mvnw.cmd test "-Dtest=CommentApiControllerTests#unauthenticatedCannotPublishComment"
```

For the frontend, from `frontend/`:

```powershell
npm ci
npm run lint -- --max-warnings=0
npm run test:coverage
npm run build
```

Vitest writes its coverage report to `frontend/coverage/`.

## CI workflow

`.github/workflows/ci.yml` runs separate backend and frontend jobs for pull requests and pushes
to `main` or `dev`.

The backend job uses Java 21 and runs `./mvnw verify`. Database integration tests use
Testcontainers and the runner's Docker engine. The frontend job uses Node.js 24 and runs
`npm ci`, ESLint with zero allowed warnings, the Vitest coverage suite, and the production build.
Both JaCoCo and frontend coverage are uploaded as workflow artifacts.

## Test coverage added

Backend tests cover authentication/session behavior, game APIs and upload validation, tag
assignment rules, forum discussions and replies, comments, profiles, profile images, and the
development data seeder. Repository/application context tests run against the same temporary
PostgreSQL database and the actual Flyway migrations.

Frontend service tests cover API error handling and the auth, comment, forum, game upload, game,
search, tag, and user service modules. Current configured frontend thresholds are 70% statements,
50% branches, 85% functions, and 70% lines.

## Troubleshooting

- **`Could not find a valid Docker environment`**: start Docker Desktop, wait for the engine to
  finish starting, then retry `./mvnw verify`.
- **PostgreSQL rejects a `TimeZone` value**: the Maven test JVM is configured to use UTC, which is
  a valid PostgreSQL timezone.
- **Image download fails**: Testcontainers needs access to pull `postgres:18` the first time. Retry
  after network access is available; Docker caches the image afterward.
- **A test fails**: inspect `backend/target/surefire-reports/` for the test report and stack trace.
- **Coverage report missing**: use `./mvnw verify`; JaCoCo's HTML report is generated during the
  `verify` phase.
