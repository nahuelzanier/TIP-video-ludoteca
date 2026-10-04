# Authentication & Authorization

Ludarium uses **server-side sessions**. There is no JWT, no OAuth and no token stored in
`localStorage`. The browser keeps a single `JSESSIONID` cookie and Spring Security resolves
the user on every request.

All the configuration lives in
`backend/src/main/java/com/tip_video_ludoteca/config/SecurityConfig.java`.

## Session model

| Setting | Value | Where it comes from |
| --- | --- | --- |
| Storage | Server-side `HttpSession` | `HttpSessionSecurityContextRepository` bean |
| Cookie | `JSESSIONID` | Servlet default |
| `HttpOnly` | `true` | `server.servlet.session.cookie.http-only` |
| `SameSite` | `lax` | `server.servlet.session.cookie.same-site` |
| Timeout | `30m` | `server.servlet.session.timeout` |
| Fixation protection | `ChangeSessionIdAuthenticationStrategy` | Applied manually on login |

The session id is rotated on every successful login because `AuthController` calls
`sessionAuthenticationStrategy.onAuthentication(...)` before persisting the security context:

```java
Authentication authentication = authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));

sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
```

### The principal is the email

`DatabaseUserDetailsService` loads users by **email**, not by username, and gives every
account the same single authority:

```java
return org.springframework.security.core.userdetails.User
        .withUsername(user.getEmail())
        .password(user.getPasswordHash())
        .roles("USER")
        .build();
```

This is why every controller reads the caller with `@AuthenticationPrincipal UserDetails` and
then does `currentUser.getUsername()`, which is the email address.

There are **no roles beyond `ROLE_USER`** and no `@PreAuthorize` anywhere in the codebase.
Ownership rules ("only the owner can publish this game") are enforced by hand inside the
service layer, not by Spring Security.

## Passwords

`SecurityConfig` exposes a delegating encoder:

```java
@Bean
PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
}
```

Hashes are stored prefixed with the algorithm, e.g. `{bcrypt}$2a$10$...`. The plaintext
password is never stored and never leaves the server.

## CSRF protection

CSRF is **enabled** with `HttpSessionCsrfTokenRepository`. Because the frontend is a separate
origin, the token cannot be read from a cookie by JavaScript, so the backend exposes it
explicitly:

```http
GET /api/auth/csrf
```

```json
{ "headerName": "X-CSRF-TOKEN", "token": "0d9f1c22-..." }
```

The client must echo the token in a request header whose **name is the one the server
returned**, on every state-changing request (`POST`, `PATCH`, `DELETE`):

```http
POST /api/auth/login
Content-Type: application/json
X-CSRF-TOKEN: 0d9f1c22-...

{ "email": "lucas2@gmail.com", "password": "lucas222" }
```

Frontend helper, `frontend/src/services/api.ts`:

```ts
export async function fetchCsrfToken(): Promise<CsrfToken> {
  const response = await fetch(`${API_BASE_URL}/api/auth/csrf`, {
    credentials: "include",
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error("No se pudo preparar la solicitud.");
  }

  return response.json();
}
```

Because the token is bound to the session, the frontend re-fetches it immediately before
each write instead of caching it at startup.

## Authorization matrix

Copied verbatim from `SecurityConfig.securityContextChain`. Everything not listed falls
through to `anyRequest().authenticated()`.

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/games` | public |
| `GET` | `/api/games/**` | public |
| `GET` | `/games/**` | public |
| `GET` | `/api/search` | public |
| `GET` | `/api/users/*/profile` | public |
| `GET` | `/api/auth/csrf` | public |
| `POST` | `/api/auth/register` | public |
| `POST` | `/api/auth/logout` | public (no-op when anonymous) |
| any | everything else | session required |

Endpoints that therefore **require** a session:

- `GET /api/auth/me`
- `POST /api/games` (upload)
- `POST /api/games/{gameId}/publish`
- `PATCH /api/users/{username}/description`
- `POST /api/games/{gameId}/comments`
- `POST /api/comments/{commentId}/replies`

> Note that `POST /api/games/{gameId}/comments` is covered by the public `GET /api/games/**`
> matcher only for the `GET` verb. The `POST` matcher lists just register and login, so
> writing a comment without a session returns `401`.

## CORS

CORS is configured once, in `config/WebConfig.java`, and Spring Security simply delegates to
it with `.cors(Customizer.withDefaults())`:

```java
registry.addMapping("/**")
        .allowedOrigins(frontendOrigin)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

`frontendOrigin` comes from `app.frontend-origin`, which defaults to
`http://localhost:5173` and can be overridden with the `FRONTEND_ORIGIN` environment
variable. `allowCredentials(true)` is what makes the `JSESSIONID` cookie cross-origin.

> Two controllers (`GameApiController`, `GameCoverController`) additionally hard-code
> `@CrossOrigin(origins = "http://localhost:5173")`, which overrides the global setting for
> those routes. See [Known Issues](Known-Issues.md).

## Response headers for the game iframe

Games are served to a cross-origin `<iframe>`, so `SecurityConfig` relaxes the framing
headers and pins the allowed ancestors:

```java
.headers(headers -> headers
    .frameOptions(frame -> frame.disable())
    .contentSecurityPolicy(csp -> csp
        .policyDirectives("frame-ancestors 'self' " + frontendOrigin)
    )
)
```

Every file served by `GameController` and `GameCoverController` also sets
`X-Content-Type-Options: nosniff`, so the browser will not re-interpret a stored blob.

## Error responses

Security failures do not always use the standard error envelope. There are three shapes:

| Situation | Status | Body |
| --- | --- | --- |
| No session on a protected route | `401` | empty (`response.sendError(401)`) |
| Session but not the owner | `403` | `{"error":"No tenés permiso para realizar esta acción."}` |
| Bad credentials on login | `401` | `{"error":"Email o contraseña incorrectos."}` |

The `401` entry point deliberately writes no body, so the frontend services treat `401` as a
signal ("return `null`" or "session expired") rather than trying to parse a message.

## Endpoints

### `POST /api/auth/register`

Public. Creates the account; it does **not** open a session. The frontend immediately calls
`/login` afterwards, which is why registering from the UI feels like a single step.

```json
{ "username": "lucas2", "email": "lucas2@gmail.com", "password": "lucas222" }
```

Validation:

| Field | Rule |
| --- | --- |
| `username` | `@Pattern("[A-Za-z0-9_]{3,30}")`, `@NotBlank` |
| `email` | `@Email`, `@NotBlank`, max 254 |
| `password` | `@NotBlank`, 8 to 72 characters |

Both `username` and `email` are `trim()`ed and lowercased (`Locale.ROOT`) before being used,
so lookups are case-insensitive.

| Status | Cause |
| --- | --- |
| `201` | Account created, body is the `UserResponse` |
| `400` | Bean validation failed (message from the first field error) |
| `409` | `Ese nombre de usuario ya existe.` or `Ese email ya está registrado.` |

### `POST /api/auth/login`

Public. Body is `{"email": "...", "password": "..."}`. On success it rotates the session id,
persists the `SecurityContext` and returns `200` with the same `UserResponse`.

| Status | Cause |
| --- | --- |
| `200` | Logged in |
| `400` | Missing email or password |
| `401` | `Email o contraseña incorrectos.` |

### `GET /api/auth/me`

Session required. Resolves the principal email back into a `UserResponse`. The frontend
returns `null` on `401` instead of throwing, so this doubles as the "am I logged in?" probe.

### `GET /api/auth/csrf`

Public. Returns `{ headerName, token }`.

### `POST /api/auth/logout`

Not a controller method. It is configured through `http.logout()`:

```java
.logout(logout -> logout
    .logoutUrl("/api/auth/logout")
    .deleteCookies("JSESSIONID")
    .logoutSuccessHandler((request, response, authentication) ->
        response.setStatus(204)))
```

Returns `204 No Content` and deletes the cookie.

> The frontend sends a CSRF header but **no** `Content-Type` and **no body** on this call,
> since the logout matcher accepts any HTTP method.

## The `UserResponse` shape

```json
{ "id": 1, "username": "lucas2", "email": "lucas2@gmail.com" }
```

Typed in the frontend as:

```ts
export interface AuthUser { id: number; username: string; email: string; }
```

The public profile endpoint returns a deliberately narrower payload without the email — see
[API](API.md#get-apiusersusernameprofile).

## How the frontend handles the session

There is no global auth context. Each page that needs the session calls `getCurrentUser()`
independently on mount:

| File | Why |
| --- | --- |
| `pages/user/user.tsx` | to decide `isOwnProfile` |
| `pages/profile/Profile.tsx` | to redirect `/profile` to `/user/:username` |
| `pages/uploadgame/UploadGame.tsx` | to guard the upload page |
| `hooks/useComments.ts` | to decide between the comment form and the "log in" notice |
| `components/navbar/navbar.tsx` | **does not** — it always renders the logout button |

Consequences worth knowing:

- Switching pages re-requests `GET /api/auth/me`; there is no cache.
- The navbar shows "Log out" to anonymous visitors. The call is harmless: the server returns
  `204` either way.
- A `401` on a write shows the inline message `Tu sesión expiró. Volvé a iniciar sesión.`
  but does not navigate to `/login`.

### CSRF prefetch is duplicated

The same "fetch the token, then POST" sequence is reimplemented in five places:

- `services/api.ts` → `fetchCsrfToken`
- `services/authService.ts` → `postAuth`, `logout`
- `services/commentService.ts` → `postComment`
- `services/userService.ts` → `updateDescription`
- `services/gameUploadService.ts` → `uploadGame`, `publishGame`

Consolidating this into one wrapper in `services/api.ts` would be a safe refactor. It is
listed in [Known Issues](Known-Issues.md).