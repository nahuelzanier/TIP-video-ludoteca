# Game isolation

Uploaded games contain untrusted HTML and JavaScript. Ludarium runs each game in a sandboxed iframe and serves its files from an origin separate from the frontend and API. This prevents game code from reading Ludarium's document, browser storage, CSRF token, or session cookie.

## Local development

Set both URLs in `frontend/.env.local`:

```env
VITE_API_URL=http://localhost:8080
VITE_GAME_CONTENT_URL=http://127.0.0.1:8080
```

The frontend uses `localhost`; the game files use `127.0.0.1`. Although both resolve to the local machine, the different host creates a different browser origin and keeps the API's host-only `JSESSIONID` cookie off game requests. Do not change the game URL to `http://localhost:8080`.

Restart Vite after changing `.env.local`. `Game.tsx` builds the iframe URL from `GAME_CONTENT_BASE_URL`; API calls continue to use `API_BASE_URL`.

`GameSection.tsx` uses:

```tsx
<iframe
  src={gameUrl}
  title="Juego"
  className="game-frame"
  sandbox="allow-scripts allow-same-origin"
/>
```

Scripts are needed by HTML5 games. `allow-same-origin` lets a game load its own files and storage under the isolated game origin. It must only be used while the game origin remains separate from both Ludarium's frontend and API. Do not add `allow-top-navigation`, `allow-forms`, or other sandbox permissions without a concrete game requirement.

## Request protections

- Spring Security stores CSRF tokens in the HTTP session and requires a valid token for state-changing requests.
- CORS allows the configured frontend origin (`app.frontend-origin` / `FRONTEND_ORIGIN`), not the game origin. Do not allow `null`, a wildcard origin, or the game host for credentialed API requests.
- The session cookie is `HttpOnly` and `SameSite=Lax`. Keep it host-only; do not set a shared cookie domain.
- Game asset URLs are public `GET` requests. All operations that change data must continue to use non-safe HTTP methods and require CSRF.

From the game frame, the browser should block attempts to read the token endpoint:

```js
fetch("http://localhost:8080/api/auth/csrf", {
  credentials: "include",
}).then((response) => response.json());
```

From `http://127.0.0.1:8080`, the browser blocks this request because that origin is not allowed by the API's CORS configuration. The backend test `publishRejectsMissingCsrfTokenEvenWhenAuthenticated` verifies that an authenticated state-changing request without a CSRF token receives `403`.

## Deployment requirements

The repository currently has no hosting, reverse-proxy, or production-profile configuration. Before deployment:

1. Serve the frontend and API from their normal production origins.
2. Serve `/games/**` from a separate **site** (a different registrable domain), for example `https://ludarium.example.com` for the app/API and `https://ludarium-games.example.net` for game files. A different port or subdomain alone is not a reliable cookie boundary.
3. Route the game site's `/games/**` requests to `GameController` (or to an equivalent static game-content service) and set `VITE_GAME_CONTENT_URL` to that game site when building the frontend.
4. Set `FRONTEND_ORIGIN` to the exact deployed frontend origin. Keep the game site out of CORS and out of the list of trusted credentialed origins.
5. Serve all production origins over HTTPS. Set the session cookie `Secure`, keep it `HttpOnly` and `SameSite=Lax`, and leave its cookie domain unset so it is host-only for the API.
6. Keep the iframe sandbox and ensure game responses' `Content-Security-Policy: frame-ancestors` permits the frontend origin.

Choose actual domains and proxy rules based on the hosting provider before deployment; the example domains above are placeholders.

## Manual verification

1. In DevTools, select the game's iframe as the Console execution context. `location.origin` should be the game origin, not the frontend or API origin.
2. From that context, the CSRF `fetch` above should be blocked by CORS; it must not return a token object.
3. The authenticated backend test without a CSRF token should return `403` and must not invoke the publication service.
4. Load each supported game and confirm its HTML, scripts, audio, WebAssembly, and other required resources still work.

See [Game Assets](Game-Assets.md), [Authentication](Authentication.md), and [Getting Started](Getting-Started.md).
