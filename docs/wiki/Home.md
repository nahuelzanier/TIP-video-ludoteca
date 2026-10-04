# Ludarium Wiki

Ludarium is a web platform where indie game developers upload, publish and discuss browser
games. Anyone can play what others have uploaded, rate it, comment on it, browse profiles and
search the catalog.

## Features

| Feature | Status |
| --- | --- |
| Paginated game catalog | working |
| Play a game in an embedded player | working |
| Registration, login, logout (session-based) | working |
| Upload a game as a ZIP, then publish it | working |
| Threaded comments with a 1-5 star rating | working |
| Public user profiles with a description | working |
| Combined search over games and users | working |
| Community tags | on `feature/tags`, **not merged** — see [Tags](Tags.md) |
| Forum, settings, random game | placeholder pages only |

## Stack

### Frontend

| | |
| --- | --- |
| React | 19 |
| TypeScript | 6.0 |
| Vite | 8.2 |
| react-router-dom | 7.18 |
| CSS | hand-written global CSS with custom properties |
| HTTP | native `fetch` — no client library |

Only three runtime dependencies. No state library, no component library, no test runner.

### Backend

| | |
| --- | --- |
| Java | 21 |
| Spring Boot | 4.1.1 |
| Spring Web MVC | `spring-boot-starter-webmvc` |
| Spring Security | sessions + CSRF, no JWT |
| Spring Data JPA | Hibernate |
| Flyway | schema migrations V1-V5 (+ V6 on `feature/tags`) |
| PostgreSQL | the only datastore, stores game binaries as `bytea` |
| Validation | Jakarta Bean Validation |

No Lombok, no MapStruct, no OpenAPI, no Actuator, no Docker.

## How it fits together

```text
Browser (React SPA)
  |  fetch, credentials: "include", CSRF header on writes
  v
Spring Boot on :8080
  |-- /api/**        JSON metadata, comments, auth, search, upload
  |-- /games/{id}/**  game files, read straight from the database
  |
  v
PostgreSQL
  app_user  games  game_files (bytea)  comments
```

Uploaded games are stored as rows in `game_files`, not as files on disk. A game is uploaded as
a ZIP in `DRAFT` state and becomes visible in the catalog only after its owner publishes it.

## Project structure

```text
TIP-video-ludoteca/
  backend/
    pom.xml
    mvnw, mvnw.cmd
    src/main/java/com/tip_video_ludoteca/
      BackendApplication.java
      auth/         AuthController
      comments/     Comment, CommentRepository, CommentService, DTOs, exceptions
      config/       SecurityConfig, WebConfig, GlobalExceptionHandler, DevDataSeeder
      controller/   CommentApiController, GameApiController, GameController,
                    GameCoverController, GameUploadController, ProfileApiController
      games/        Game, GameFile, GameFileId, GameRepository, GameService,
                    GameUploadService, GameStatus, exceptions
      search/       SearchController
      users/        User, UserRepository, UserProfileService, DatabaseUserDetailsService
    src/main/resources/
      application.properties
      application-dev.properties
      db/migration/  V1__create_users.sql ... V5__create_comments.sql
    src/test/java/com/tip_video_ludoteca/   8 test classes, 56 tests
  frontend/
    package.json
    vite.config.ts
    .env.local          (gitignored: VITE_API_URL)
    src/
      App.tsx           routes
      services/         the only place that calls fetch
      types/            Game, Comment, Search, User
      hooks/            useComments
      pages/            Home, Game, AuthPage, Search, User, Profile,
                        UploadGame, Forum, Settings, RandomGame
      components/       comments/, games/, navbar/, profile/
      index.css         design tokens
  docs/wiki/            this wiki
  mock/LudariumMU.png   design mockup, unreferenced
  .github/workflows/ci.yml
  README.md
  LICENSE               MIT
```

## Screens

| Route | Screen |
| --- | --- |
| `/` | catalog grid with pagination |
| `/game/:id` | game page: player, comments, ratings |
| `/login`, `/register` | auth form, one component in two modes |
| `/search` | combined results for games and users, paged per section |
| `/user/:username` | public profile with description and activity |
| `/profile` | redirect shim to `/user/{username}` |
| `/games/upload` | upload a ZIP and publish it |
| `/forum`, `/settings`, `/randomgame` | placeholders |

## Documentation map

| Page | Contents |
| --- | --- |
| [Getting Started](Getting-Started.md) | prerequisites, PostgreSQL setup, running both halves |
| [Architecture](Architecture.md) | packages, request flows, error handling, design decisions |
| [Database](Database.md) | PostgreSQL, the five Flyway migrations, the entity model |
| [API](API.md) | every endpoint with payloads and status codes |
| [Authentication](Authentication.md) | sessions, CSRF, authorization matrix, CORS |
| [Game Upload](Game-Upload.md) | the ZIP → DRAFT → PUBLISHED flow and its limits |
| [Game Assets](Game-Assets.md) | how game files are stored and served |
| [Frontend](Frontend.md) | routes, services, types, state, styling, linting |
| [Tags](Tags.md) | the tags feature on `feature/tags`, not merged |
| [Testing](Testing.md) | the 56-test backend suite, the CI workflow, the gaps |
| [Known Issues](Known-Issues.md) | bugs, dead code and technical debt, prioritised |

## Project status

`main` is a week behind the working branches and still contains the retired JSON catalog plus
~142 MB of Godot binaries. The current code is on `featureComentarios`. Read
[Known Issues](Known-Issues.md#branch-divergence) before deploying or merging anything.

There is no deployment configuration in the repository: no Dockerfile, no compose file, no
production Spring profile.

## Contributors

- Nahuel Zanier
- Lucas Sanguinetti

## License

MIT. See [LICENSE](../../LICENSE).