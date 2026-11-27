# Frontend

The frontend is a React single-page application built with TypeScript and Vite. It uses
React Router, native `fetch`, hand-written CSS, and Vitest. API requests are centralized in
`src/services/`; session-based calls include credentials and write requests fetch a CSRF token.

## Commands

Run these from `frontend/`:

| Command | Purpose |
| --- | --- |
| `npm run dev` | Start Vite at `http://localhost:5173` |
| `npm run lint` | Run ESLint |
| `npm test` | Run Vitest |
| `npm run test:coverage` | Run Vitest with coverage |
| `npm run build` | Type-check and create the production build |
| `npm run preview` | Serve the production build locally |

Set `VITE_API_URL=http://localhost:8080` and
`VITE_GAME_CONTENT_URL=http://127.0.0.1:8080` in `frontend/.env.local`. The separate game
content origin supports iframe isolation; see [Game Isolation](Game-Isolation.md).

## Routes

Routes are declared in `src/App.tsx` under a shared navbar layout.

| Path | Page |
| --- | --- |
| `/` | Home/catalog |
| `/game/:id` | Game details, player, comments, rating and owner customization |
| `/search` | Search games and users |
| `/login`, `/register` | Authentication |
| `/profile`, `/user/:username` | Current-user redirect and public profile |
| `/settings` | Preferences and account settings |
| `/games/upload` | Upload a draft |
| `/admin/setup`, `/admin/review` | First-admin setup and game/admin review |
| `/forum` | Discussion list |
| `/forum/new` | Create discussion |
| `/forum/:discussionId` | Discussion detail, replies and rating |
| `/noticias` | News feed |
| `/noticias/nueva` | Publish news |
| `/noticias/:id` | News detail and comments |
| `/favorites` | Favorite games |

Some write pages require a session and enforce permissions in the backend. The frontend does
not replace server authorization.

## Feature notes

- **Settings:** `settingsService` loads and saves preferences to the user's `user_settings`
  record. After authentication, `MainLayout` applies the persisted theme and language.
  Translation strings are provided through the frontend i18n utility; coverage is partial.
- **Game customization:** the game owner can change background, accent and text colors,
  background style and font. Preferences are loaded with game details and saved through the
  customization endpoint. Authorization compares stable user IDs server-side.
- **Forum:** users can browse discussions, create one for a published game, read replies,
  reply and rate a discussion. Tags are served from the backend.
- **News:** authenticated users can read their followed creators' feed, publish posts and
  follow/unfollow from profiles; public detail pages include comments.
- **Game uploads:** an upload creates a draft. Administrators test and publish it in the
  review page; authors do not publish their own drafts.

## Styling and theme

Global design tokens and base styles live in `src/index.css`; theme overrides are in
`src/theme.css`. Feature styles are kept with their page/component folders. Prefer existing
tokens and component patterns when adding pages so light/dark behavior remains consistent.

See [Settings](Settings.md), [Game Customization](Game-Customization.md),
[News and Follows](News-and-Follows.md), and [Testing](Testing.md) for feature-specific details.
