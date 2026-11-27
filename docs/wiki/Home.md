# Ludarium Wiki

Ludarium is a web platform where indie game developers upload browser games and players
discover, play, rate and discuss them. Authors submit drafts; administrators review and publish
them. The community can also publish news, follow creators, and join game discussions.

## Features

| Feature | Status |
| --- | --- |
| Paginated published-game catalog, search and recommendations | implemented |
| HTML game upload as a draft and admin review/publication | implemented |
| Game comments, replies, 1–5 ratings and favorites | implemented |
| Per-game appearance customization (colors and fonts) | implemented |
| Profiles, avatars, user settings and account updates | implemented |
| Forum discussions, tags, replies and ratings | implemented |
| News feed, authoring, comments and creator follow/unfollow | implemented |
| Save states for arbitrary HTML games | not implemented |

## Stack

- **Frontend:** React 19, TypeScript, Vite, React Router, hand-written CSS, native `fetch`.
- **Backend:** Java 21, Spring Boot 4, Spring Security sessions + CSRF, Spring Data JPA,
  Jakarta Validation.
- **Database:** PostgreSQL; Flyway owns schema changes. Uploaded game assets and user media
  are stored in PostgreSQL as `bytea`.
- **Frontend tests:** Vitest; **backend tests:** JUnit/Spring tests with PostgreSQL Testcontainers.

## Request flow

```text
React SPA -- fetch + session cookie + CSRF on writes --> Spring Boot API --> PostgreSQL
     |                                                     |
     +-- separate game-content origin <-- game files ------+
```

New uploads are private drafts. Administrators can load draft games for review and publish
them from `/admin/review`; only published games appear in the public catalog. Read
[Authentication](Authentication.md) for role setup and [Game Upload](Game-Upload.md) for the
review flow.

## Main routes

| Route | Purpose |
| --- | --- |
| `/` | Published game catalog |
| `/game/:id` | Play a game, customize its appearance (owner), comment and rate |
| `/search` | Search games and users |
| `/forum`, `/forum/new`, `/forum/:discussionId` | Browse, create and read discussions |
| `/noticias`, `/noticias/nueva`, `/noticias/:id` | Followed creators' news, publish and read |
| `/user/:username`, `/profile` | Public profile and signed-in user's profile |
| `/settings` | Persistent preferences and account settings |
| `/games/upload` | Submit a game draft |
| `/admin/setup`, `/admin/review` | Initial admin setup and draft/role management |

## Documentation map

| Page | Contents |
| --- | --- |
| [Getting Started](Getting-Started.md) | prerequisites, database setup and running locally |
| [Architecture](Architecture.md) | packages, request flows and design decisions |
| [Database](Database.md) | PostgreSQL, migrations and entity relationships |
| [API](API.md) | endpoints, access and payload conventions |
| [Authentication](Authentication.md) | sessions, CSRF, roles and authorization |
| [Game Upload](Game-Upload.md) | upload, draft review and publication |
| [Game Assets](Game-Assets.md) | storage and serving game files |
| [Game Isolation](Game-Isolation.md) | iframe origin and browser security |
| [Frontend](Frontend.md) | routes, services, settings, styling and localization |
| [Settings](Settings.md) | account data and persisted preferences |
| [Game Customization](Game-Customization.md) | per-game appearance options and ownership |
| [News and Follows](News-and-Follows.md) | creator news, feeds, follows and comments |
| [Tags](Tags.md) | game and discussion tags |
| [Testing](Testing.md) | local test commands and GitHub Actions |
| [Known Issues](Known-Issues.md) | current caveats and technical debt |

## Contributors and license

- Nahuel Zanier
- Lucas Sanguinetti

MIT. See [LICENSE](../../LICENSE).
