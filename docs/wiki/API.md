# API Reference

Base URL in development: `http://localhost:8080`

Every endpoint is under `/api` except the game file routes, which are under `/games`. The
catalog, the game files, the search and the public profiles are readable without a session.

## Contents

- [Conventions](#conventions)
- [Games](#games)
- [Game files](#game-files)
- [Comments](#comments)
- [Profiles](#profiles)
- [Search](#search)
- [Authentication](#authentication)
- [Tag endpoints (not merged)](#tag-endpoints-not-merged)

## Conventions

### Authentication

Server-side session via the `JSESSIONID` cookie. Every session-dependent request needs
`credentials: "include"`. There is no token in a header or in storage. See
[Authentication](Authentication.md).

### CSRF

All `POST`, `PATCH` and `DELETE` requests need a token. Fetch it and echo it back in the
header the server names:

```http
GET /api/auth/csrf

{ "headerName": "X-CSRF-TOKEN", "token": "0d9f1c22-..." }
```

```http
POST /api/games/game1/comments
Content-Type: application/json
X-CSRF-TOKEN: 0d9f1c22-...
```

### Error format

Most errors use one envelope:

```json
{ "error": "El juego no existe." }
```

There are three exceptions to know about:

| Case | Body |
| --- | --- |
| `401` from Spring Security | empty |
| `403` from Spring Security | `{"error":"No tenés permiso para realizar esta acción."}` |
| Errors thrown as `ResponseStatusException` | a Spring `ProblemDetail` with a `detail` field |

`ResponseStatusException` is used by `GameUploadService` (all its validation) and by the tag
endpoints, so those responses look like:

```json
{ "type": "...", "status": 400, "error": "Bad Request", "detail": "Choose a ZIP file." }
```

### Endpoint summary

| Method | Path | Auth |
| --- | --- | --- |
| `GET` | `/api/games` | public |
| `POST` | `/api/games` | **session** |
| `GET` | `/api/games/{gameId}/cover` | public |
| `POST` | `/api/games/{gameId}/publish` | **session** |
| `GET` | `/games/{gameId}/**` | public |
| `GET` | `/api/games/{gameId}/comments` | public |
| `POST` | `/api/games/{gameId}/comments` | **session** |
| `POST` | `/api/comments/{commentId}/replies` | **session** |
| `GET` | `/api/users/{username}/profile` | public |
| `PATCH` | `/api/users/{username}/description` | **session** |
| `GET` | `/api/search` | public |
| `POST` | `/api/auth/register` | public |
| `POST` | `/api/auth/login` | public |
| `GET` | `/api/auth/me` | **session** |
| `GET` | `/api/auth/csrf` | public |
| `POST` | `/api/auth/logout` | public |

---

# Games

## GET /api/games

The catalog. Only `PUBLISHED` games, newest first.

```http
GET http://localhost:8080/api/games?page=0&size=12
```

| Parameter | Default | Range |
| --- | --- | --- |
| `page` | `0` | clamped to `>= 0` |
| `size` | `12` | clamped to `1..24` |

### Response

```json
{
  "content": [
    {
      "id": "3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35",
      "title": "Blasteroids",
      "image": "/api/games/3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35/cover",
      "description": "Un clon de Asteroids."
    }
  ],
  "page": 0,
  "size": 12,
  "totalElements": 3,
  "totalPages": 1,
  "hasNext": false,
  "hasPrevious": false
}
```

The `image` field is a **relative API path**, not a file path. The frontend resolves it with
`new URL(game.image, API_BASE_URL)`.

Note this endpoint returns a **paged object, not a bare array**. It previously returned an
array read from `games.json`; that file and that shape are gone.

## POST /api/games

Upload a game. Requires a session and a CSRF token. The owner is taken from the session.

```http
POST /api/games
Content-Type: multipart/form-data
X-CSRF-TOKEN: 0d9f1c22-...

title=Blasteroids
description=Un clon de Asteroids.
cover=@blasteroids.png
archive=@blasteroids.zip
```

| Field | Required | Rules |
| --- | --- | --- |
| `title` | yes | 1-120 characters after trimming |
| `description` | no | max 5000 characters, defaults to `""` |
| `cover` | yes | PNG or JPEG by magic bytes, max 5 MB |
| `archive` | yes | ZIP, max 100 MB, must contain `index.html` at its root |

```json
{ "id": "3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35", "title": "Blasteroids", "filesStored": 11 }
```

The game is created with `status = "DRAFT"` and does not appear in `GET /api/games` until it
is published.

| Status | Cause |
| --- | --- |
| `201` | Stored as a draft |
| `400` | Any validation failure (17 distinct messages, all in English) |
| `401` | No session, or the user no longer exists |
| `403` | Missing or invalid CSRF token |

Full validation table and the limits: [Game Upload](Game-Upload.md#validation-limits).

## POST /api/games/{gameId}/publish

Moves a game from `DRAFT` to `PUBLISHED`. Requires a session and a CSRF token. Only the owner
may publish. Idempotent.

```http
POST http://localhost:8080/api/games/3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35/publish
X-CSRF-TOKEN: 0d9f1c22-...
```

```json
{
  "id": "3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35",
  "title": "Blasteroids",
  "status": "PUBLISHED"
}
```

| Status | Cause |
| --- | --- |
| `200` | Published, or already published |
| `401` | No session |
| `403` | `No podés publicar el juego de otro usuario.` |
| `404` | `El juego no existe.` |

---

# Game files

## GET /api/games/{gameId}/cover

The cover image, as bytes.

```http
GET http://localhost:8080/api/games/3f9c2a41-.../cover
```

```text
HTTP/1.1 200 OK
Content-Type: image/png
X-Content-Type-Options: nosniff
```

| Status | Cause |
| --- | --- |
| `200` | The image, with the content type recorded at upload time |
| `404` | Unknown game, or no cover stored |

The cover is stored as an ordinary `game_files` row at the reserved path
`__metadata__/thumbnail`.

## GET /games/{gameId}/**

Any file belonging to a game. This is what the `<iframe>` player requests.

```http
GET http://localhost:8080/games/3f9c2a41-.../index.html
```

```text
HTTP/1.1 200 OK
Content-Type: text/html; charset=UTF-8
X-Content-Type-Options: nosniff
```

The requested path is URI-decoded, normalized, and looked up by `(gameId, path)`. Because the
lookup is keyed by both, one game's request can never reach another game's row.

| Status | Cause |
| --- | --- |
| `200` | The file, with its stored content type |
| `400` | Blank, absolute or `..`-leading path |
| `404` | No such file for this game |

Content types are decided once at upload time from the extension:

| Extension | Content type |
| --- | --- |
| `.html` | `text/html; charset=UTF-8` |
| `.js` | `text/javascript; charset=UTF-8` |
| `.css` | `text/css; charset=UTF-8` |
| `.json` | `application/json` |
| `.wasm` | `application/wasm` |
| anything else | `URLConnection.guessContentTypeFromName`, else `application/octet-stream` |

---

# Comments

## GET /api/games/{gameId}/comments

Public. Roots newest first, replies oldest first, nested one level deep.

```http
GET http://localhost:8080/api/games/3f9c2a41-.../comments
```

```json
[
  {
    "id": 12,
    "content": "El control del salto es muy bueno.",
    "rating": 5,
    "createdAt": "2026-10-01T12:00:00Z",
    "username": "lucas2",
    "profileImageUrl": null,
    "isGameAuthor": true,
    "replies": [
      {
        "id": 13,
        "content": "Totalmente de acuerdo.",
        "rating": null,
        "createdAt": "2026-10-01T13:00:00Z",
        "username": "nahuel",
        "profileImageUrl": null,
        "isGameAuthor": false,
        "replies": []
      }
    ]
  }
]
```

| Field | Type | Notes |
| --- | --- | --- |
| `id` | number | |
| `content` | string | max 1000 |
| `rating` | number \| null | 1-5, roots only |
| `createdAt` | string | ISO-8601 instant |
| `username` | string | display name only |
| `profileImageUrl` | string \| null | |
| `isGameAuthor` | boolean | computed by the backend, never sent by the client |
| `replies` | array | always `[]` on a reply |

`isGameAuthor` is computed server-side by comparing the comment's author with the game owner.
The response **never** contains an email or a password hash.

| Status | Cause |
| --- | --- |
| `200` | The thread |
| `404` | `El juego no existe.` |

## POST /api/games/{gameId}/comments

Publish a root comment. Requires a session and a CSRF token.

```http
POST http://localhost:8080/api/games/3f9c2a41-.../comments
Content-Type: application/json
X-CSRF-TOKEN: 0d9f1c22-...

{ "content": "Me gustó mucho.", "rating": 4 }
```

The author is **always** taken from the security context, never from the body. `rating` is
optional and must be 1-5.

| Status | Cause |
| --- | --- |
| `201` | Comment published |
| `400` | `Escribí un comentario antes de publicarlo.`, content over 1000 characters, or a rating outside 1-5 |
| `401` | No session |
| `404` | `El juego no existe.` |

## POST /api/comments/{commentId}/replies

Publish a reply. Requires a session and a CSRF token.

```http
POST http://localhost:8080/api/comments/12/replies
Content-Type: application/json
X-CSRF-TOKEN: 0d9f1c22-...

{ "content": "Yo lo jugué y me pasó igual." }
```

Replies cannot carry a rating:

```json
{ "error": "Las respuestas no pueden tener valoración. Valorá el comentario original." }
```

A reply **to a reply** is stored against the root comment, so the nesting stays one level
deep no matter what the client posts.

| Status | Cause |
| --- | --- |
| `201` | Reply published |
| `400` | Blank content, content over 1000 characters, or a `rating` was sent |
| `401` | No session |
| `404` | `El comentario no existe.` |

---

# Profiles

## GET /api/users/{username}/profile

Public. Deliberately narrow: **no email, no password hash**.

```http
GET http://localhost:8080/api/users/lucas2/profile
```

```json
{ "id": 1, "username": "lucas2", "description": "Me gustan los puzzles." }
```

`description` is `null` when never set. Lookups are case-insensitive.

| Status | Cause |
| --- | --- |
| `200` | The profile |
| `404` | `Usuario no encontrado.` |

## PATCH /api/users/{username}/description

Edit the profile description. Requires a session and a CSRF token. Only the owner, compared
case-insensitively.

```http
PATCH http://localhost:8080/api/users/lucas2/description
Content-Type: application/json
X-CSRF-TOKEN: 0d9f1c22-...

{ "description": "Me gustan los puzzles." }
```

The value is trimmed, and an empty string is stored as `null` — so submitting a blank field
clears the description.

| Status | Cause |
| --- | --- |
| `200` | The updated profile, same shape as the GET |
| `400` | `La descripción no puede superar los 500 caracteres.` |
| `401` | No session |
| `403` | `No podés editar el perfil de otro usuario.` |
| `404` | `Usuario no encontrado.` |

---

# Search

## GET /api/search

Combined search over game titles and usernames, paginated independently.

```http
GET http://localhost:8080/api/search?q=bla&gamesPage=0&usersPage=0&size=9
```

| Parameter | Required | Default | Range |
| --- | --- | --- | --- |
| `q` | **yes** | — | 2 to 100 characters after trimming |
| `gamesPage` | no | `0` | `>= 0` |
| `usersPage` | no | `0` | `>= 0` |
| `size` | no | `9` | 1 to 50, per section |

### Response

```json
{
  "query": "bla",
  "games": {
    "items": [
      {
        "id": "3f9c2a41-...",
        "title": "Blasteroids",
        "image": "/api/games/3f9c2a41-.../cover",
        "description": "Un clon de Asteroids."
      }
    ],
    "page": 0,
    "size": 9,
    "totalItems": 1,
    "totalPages": 1
  },
  "users": {
    "items": [ { "id": 1, "username": "lucas2" } ],
    "page": 0,
    "size": 9,
    "totalItems": 1,
    "totalPages": 1
  }
}
```

Both sections are always present. The games section carries `id`, `title`, `image` and
`description`; the users section carries only `id` and `username`.

Note the field is **`totalItems`** here, whereas `GET /api/games` uses `totalElements` and also
returns `hasNext` / `hasPrevious`. Two shapes for the same concept — see
[Known Issues](Known-Issues.md).

Matching is case-insensitive substring (`ILIKE`), backed by the `pg_trgm` GIN indexes from
migration `V4`.

| Status | Cause |
| --- | --- |
| `200` | Results, possibly empty |
| `400` | `Escribí un término para buscar.`, `El término debe tener entre 2 y 100 caracteres.`, `El número de página no puede ser negativo.`, `El tamaño de página debe estar entre 1 y 50.` |

---

# Authentication

Summarised here; see [Authentication](Authentication.md) for the model.

## POST /api/auth/register

Public. Creates an account. **Does not** open a session — the frontend calls `/login` next.

```json
{ "username": "lucas2", "email": "lucas2@gmail.com", "password": "lucas222" }
```

| Field | Rules |
| --- | --- |
| `username` | `[A-Za-z0-9_]{3,30}` |
| `email` | valid email, max 254 |
| `password` | 8 to 72 characters |

```json
{ "id": 1, "username": "lucas2", "email": "lucas2@gmail.com" }
```

| Status | Cause |
| --- | --- |
| `201` | Created |
| `400` | Validation failed |
| `409` | `Ese nombre de usuario ya existe.` or `Ese email ya está registrado.` |

## POST /api/auth/login

Public. Needs a CSRF token.

```json
{ "email": "lucas2@gmail.com", "password": "lucas222" }
```

On success the session id is rotated and the response is the same `UserResponse`.

| Status | Cause |
| --- | --- |
| `200` | Logged in |
| `400` | Missing email or password |
| `401` | `Email o contraseña incorrectos.` |

## GET /api/auth/me

Session required.

```json
{ "id": 1, "username": "lucas2", "email": "lucas2@gmail.com" }
```

Returns `401` with an empty body when there is no session. The frontend treats that as `null`
rather than an error, which makes this the "am I logged in?" probe.

## GET /api/auth/csrf

Public.

```json
{ "headerName": "X-CSRF-TOKEN", "token": "0d9f1c22-..." }
```

## POST /api/auth/logout

Configured through `http.logout()`, not a controller method. Deletes the `JSESSIONID` cookie.

| Status | Cause |
| --- | --- |
| `204` | Always, including when there was no session |

---

# Tag endpoints (not merged)

> These exist only on `origin/feature/tags` and are **not available** on `main`, `dev` or
> `featureComentarios`. Full explanation in [Tags](Tags.md).

## GET /api/tags

Public. The 15 fixed tags, alphabetically.

```json
[ { "id": 1, "name": "Action", "slug": "action" } ]
```

## GET /api/games/{gameId}/tags

Public. Ordered by assignment count descending, then by first assignment, then by name.

```json
[ { "tagId": 7, "name": "Puzzle", "slug": "puzzle",
    "assignmentCount": 3, "firstAssignedAt": "2026-10-02T09:15:00Z" } ]
```

`404` for an unknown game **or a draft game** — a draft is indistinguishable from a missing
one.

## GET /api/games/{gameId}/tags/mine

**Requires a session.** The ids the caller already assigned:

```json
[7, 12]
```

## POST /api/games/{gameId}/tags

Requires a session and a CSRF token.

```http
POST /api/games/3f9c2a41-.../tags
X-CSRF-TOKEN: 0d9f1c22-...

{ "tagId": 7 }
```

`201` returns the **full updated tag list**, not just the new assignment.

| Status | Cause |
| --- | --- |
| `201` | Assigned |
| `400` | `A tag ID is required.` |
| `401` | No session |
| `404` | `Game not found.` or `Tag not found.` |
| `409` | `You have already assigned this tag to this game.` |

## Related

- [Architecture](Architecture.md) — how these flows are wired
- [Game Upload](Game-Upload.md) — the upload endpoint in depth
- [Authentication](Authentication.md) — sessions, CSRF and the authorization matrix
- [Known Issues](Known-Issues.md) — the inconsistent error bodies described above