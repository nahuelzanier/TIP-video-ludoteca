# API Reference

The backend exposes a minimal API focused on game metadata and static content delivery.

## GET /api/games

Returns the full game catalog as a JSON array.

### Endpoint

```http
GET http://localhost:8080/api/games
```

### Response example

```json
[
  {
    "id": "game1",
    "title": "Blasteroids",
    "image": "/images/game1.png",
    "description": "Un clon de Asteroids."
  }
]
```

### Notes

- The response is read from `backend/src/main/resources/games.json`.
- The endpoint is configured with CORS enabled for the frontend origin `http://localhost:5173`.

## GET /games/{gameId}/**

Serves any asset or file inside a game folder.

### Example

```http
GET http://localhost:8080/games/game1/index.html
```

This route resolves the request against:

```text
backend/gamesData/game1/
```

### Security behavior

The backend validates that the requested file remains inside the selected game directory. This prevents path traversal outside the intended folder.

## Current API scope

This API is intentionally small and built for a catalog-driven game portal. It is not a full user system or content management backend yet; it mainly exposes catalog metadata and static game bundles.

## GET /api/games/{gameId}/comments

Returns the comments of a game, newest first, with their replies nested one level deep.
Replies keep the conversation order (oldest to newest). The endpoint is public.

```http
GET http://localhost:8080/api/games/game1/comments
```

### Response example

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

`isGameAuthor` is always computed on the backend by comparing the comment author with the
game owner. Responses never include the email or the password hash.

Returns `404` with `{"error":"El juego no existe."}` when the game does not exist.

## POST /api/games/{gameId}/comments

Publishes a root comment. Requires an authenticated session (session cookie + CSRF token).
The author is always taken from the security context, never from the request body.

```http
POST http://localhost:8080/api/games/game1/comments
Content-Type: application/json

{ "content": "Me gustó mucho.", "rating": 4 }
```

`rating` is optional and must be between 1 and 5. Replies to replies are always stored
against the root comment, so the nesting stays one level deep.

### Status codes

| Status | Cause |
| --- | --- |
| `201` | Comment published |
| `400` | Empty content, content over 1000 characters, or a rating outside 1-5 |
| `401` | No active session |
| `404` | The game does not exist |

## POST /api/comments/{commentId}/replies

Publishes a reply to a comment. Replies cannot carry a rating.

```http
POST http://localhost:8080/api/comments/12/replies
Content-Type: application/json

{ "content": "Yo lo jugué y me pasó igual." }
```

| Status | Cause |
| --- | --- |
| `201` | Reply published |
| `400` | Empty content, content over 1000 characters, or a `rating` was sent |
| `401` | No active session |
| `404` | The comment does not exist |

## POST /api/games/{gameId}/publish

Moves a game from `DRAFT` to `PUBLISHED` so it shows up in the catalog
(`GET /api/games`). Only the game owner can publish it. The operation is
idempotent: publishing an already published game returns `200` again.

```http
POST http://localhost:8080/api/games/game1/publish
```

### Response example

```json
{
  "id": "game1",
  "title": "Mi juego",
  "status": "PUBLISHED"
}
```

### Status codes

| Status | Cause |
| --- | --- |
| `200` | Game published (or already published) |
| `401` | No active session |
| `403` | The authenticated user is not the game owner |
| `404` | The game does not exist |
