# Game Upload & Publishing

Anyone with an account can upload a browser game and then publish it to the catalog. The whole
flow lives in `games/GameUploadService.java` and `games/GameService.java`.

## Lifecycle

```text
        POST /api/games  (multipart: title, description, cover, archive)
                          |
                          v
                     status = DRAFT        <- invisible in the catalog
                          |
        POST /api/games/{gameId}/publish   (owner only)
                          |
                          v
                     status = PUBLISHED    <- appears in GET /api/games
```

A draft game is stored completely but never returned by `GET /api/games`, `GET /api/search`
or the tag endpoints, all of which filter on `GameStatus.PUBLISHED`.

`Game.status` only ever moves forward. There is no unpublish and no edit endpoint: `Game`
exposes no setters for `title` or `description`, only `assignOwner(User)` and `publish()`.

## Step 1 — Upload

```http
POST /api/games
Content-Type: multipart/form-data
X-CSRF-TOKEN: 0d9f1c22-...

title=Blasteroids
description=Un clon de Asteroids.
cover=@blasteroids.png
archive=@blasteroids.zip
```

Requires a session and a CSRF token. The owner is taken from `@AuthenticationPrincipal`, never
from the request body.

### Response

```http
201 Created
```

```json
{ "id": "3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35", "title": "Blasteroids", "filesStored": 11 }
```

`filesStored` counts the ZIP entries **plus one** for the stored cover.

### Status codes

| Status | Cause |
| --- | --- |
| `201` | Game stored as `DRAFT` |
| `400` | Any validation failure (see below) |
| `401` | No session, or the authenticated user no longer exists in the database |
| `403` | Missing or invalid CSRF token |

All validation messages are **in English**, unlike the rest of the API:

| Message | Trigger |
| --- | --- |
| `Title must contain between 1 and 120 characters.` | Title blank after trim, or over 120 chars |
| `Description cannot exceed 5000 characters.` | Description over 5000 chars |
| `Choose a cover image.` | Missing or empty `cover` |
| `The cover image cannot exceed 5 MB.` | Cover over 5 MB |
| `The cover must be a PNG or JPEG image.` | Cover is not a PNG or JPEG (magic-byte check) |
| `Choose a ZIP file.` | Missing or empty `archive` |
| `The ZIP file exceeds the 100 MB limit.` | `archive.getSize() > 100 MB` |
| `Could not read the ZIP file.` | Corrupt archive (`IOException`) |
| `The ZIP contains no files.` | Archive has no non-directory entries |
| `The ZIP contains too many files.` | More than 2000 entries |
| `A file in the ZIP exceeds the size limit.` | A single entry expands past 75 MB |
| `The extracted ZIP exceeds the 100 MB limit.` | Total uncompressed size past 100 MB |
| `The ZIP contains a duplicate file path.` | Two entries normalize to the same path |
| `The ZIP contains an invalid file path.` | Absolute path, `..`, backslash, colon, `.` segment |
| `A file path in the ZIP is too long.` | Normalized path over 500 chars |
| `The ZIP must contain index.html at its root. ZIP the contents of the game folder.` | No root `index.html` |
| `The ZIP uses a reserved path for the cover image.` | Archive contains `__metadata__/thumbnail` |

> These come from `ResponseStatusException`, which is **not** handled by
> `GlobalExceptionHandler`. The body is therefore a Spring `ProblemDetail`
> (`{"status":400,"error":"Bad Request","detail":"..."}`) instead of the usual
> `{"error":"..."}` envelope. `services/gameUploadService.ts` parses `detail`, `message` and
> `error` in that order to cope.

### Validation limits

```java
private static final long MAX_ARCHIVE_BYTES   = 100L * 1024 * 1024;
private static final long MAX_EXTRACTED_BYTES = 100L * 1024 * 1024;
private static final long MAX_FILE_BYTES      =  75L * 1024 * 1024;
private static final long MAX_COVER_BYTES     =   5L * 1024 * 1024;
private static final int  MAX_FILES           = 2000;
private static final String COVER_FILE_PATH   = "__metadata__/thumbnail";
```

The archive size and the extracted size are tracked separately, which is the zip-bomb guard: a
small ZIP that expands to gigabytes hits `MAX_EXTRACTED_BYTES` and is rejected mid-stream.

### Path validation

`validatePath` runs on every ZIP entry and rejects anything that could escape the game's
virtual root:

```java
if (entryName == null
        || entryName.isBlank()
        || entryName.startsWith("/")
        || entryName.contains("\\")
        || entryName.contains(":")) {
    throw badRequest("The ZIP contains an invalid file path.");
}

for (String part : entryName.split("/")) {
    if (part.isBlank() || part.equals(".") || part.equals("..")) {
        throw badRequest("The ZIP contains an invalid file path.");
    }
}
```

It then normalizes with `Paths.get(...).normalize()` and rejects absolute paths, paths
starting with `..`, and anything longer than 500 characters.

### Cover detection

The cover is validated by **magic bytes**, not by filename or by the browser-supplied MIME
type:

```java
boolean isPng = content.length >= 8
        && content[0] == (byte) 0x89 && content[1] == 0x50
        && content[2] == 0x4E && content[3] == 0x47
        && content[4] == 0x0D && content[5] == 0x0A
        && content[6] == 0x1A && content[7] == 0x0A;

boolean isJpeg = content.length >= 3
        && content[0] == (byte) 0xFF && content[1] == (byte) 0xD8 && content[2] == (byte) 0xFF;
```

The stored content type (`image/png` or `image/jpeg`) is what gets replayed later by
`GameCoverController`, so a file renamed to `.exe` cannot be served as HTML.

### Content type detection

Each stored file gets its content type at upload time and it is **replayed verbatim** on
every request:

```java
if (lowerPath.endsWith(".html")) return "text/html; charset=UTF-8";
if (lowerPath.endsWith(".js"))   return "text/javascript; charset=UTF-8";
if (lowerPath.endsWith(".css"))  return "text/css; charset=UTF-8";
if (lowerPath.endsWith(".json")) return "application/json";
if (lowerPath.endsWith(".wasm")) return "application/wasm";

String guessed = URLConnection.guessContentTypeFromName(path);
return guessed != null ? guessed : "application/octet-stream";
```

Godot HTML5 exports need the explicit `.wasm` case because `URLConnection` does not know it.

### What gets written

```java
String gameId = UUID.randomUUID().toString();

Game game = new Game(
        gameId,
        owner,
        cleanTitle,
        cleanDescription,
        "/api/games/" + gameId + "/cover",
        "index.html",
        GameStatus.DRAFT
);

games.save(game);
gameFiles.saveAll(rows);
```

The `coverImageUrl` is written as an **API path**, not a file path, so the catalog can hand it
straight to an `<img src>`. The frontend resolves it to an absolute URL in
`services/gameService.ts`.

## Step 2 — Publish

```http
POST /api/games/{gameId}/publish
X-CSRF-TOKEN: 0d9f1c22-...
```

```json
{ "id": "3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35", "title": "Blasteroids", "status": "PUBLISHED" }
```

Ownership is enforced by hand in `GameService.publish`:

```java
if (!game.getOwner().getId().equals(user.getId())) {
    throw new GameEditForbiddenException();
}
```

The operation is **idempotent**: calling it on an already published game returns `200` again
without changing anything.

| Status | Cause |
| --- | --- |
| `200` | Published, or already published |
| `401` | No session |
| `403` | `No podés publicar el juego de otro usuario.` |
| `404` | `El juego no existe.` |

## Step 3 — Serving

Once published, the game is playable at:

```text
GET /games/{gameId}/index.html
```

`GameController` maps `/{gameId}/**`, decodes the URI, strips the
`{contextPath}/games/{gameId}/` prefix, normalizes what is left and looks the result up by
`(gameId, path)`:

```java
String databasePath = normalized.toString().replace("\\", "/");

return gameFiles.findById_GameIdAndId_Path(gameId, databasePath)
        .map(this::toResponse)
        .orElseGet(() -> ResponseEntity.notFound().build());
```

Blank, absolute and `..`-leading paths return `400`; anything not present in `game_files`
returns `404`. Because the lookup is keyed by game id **and** path, one game's asset request
can never reach another game's row.

The cover is served by a separate endpoint rather than through `/games/**`:

```http
GET /api/games/{gameId}/cover
```

which reads the row at the reserved path `__metadata__/thumbnail`. Both endpoints send
`X-Content-Type-Options: nosniff`.

## Multipart limits

Two layers. Servlet level in `application.properties`:

```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=105MB
```

Service level in `GameUploadService`, which re-checks each file and the total. Exceeding the
servlet limit raises `MaxUploadSizeExceededException`, which `GlobalExceptionHandler` does not
handle — the response falls back to Spring's default body.

## The frontend flow

`frontend/src/pages/uploadgame/UploadGame.tsx` runs the whole thing as two buttons:

1. **Upload game** → `uploadGame(title, description, cover, archive)` from
   `services/gameUploadService.ts`. On success it shows
   `"Blasteroids" was uploaded. 11 files were stored as a draft.` and remembers the new id.
2. **Publish game** → `publishGame(id)`. On success it shows
   `"Blasteroids" is now published and visible on the home page.`

Client-side validation before the request:

| Field | Rule |
| --- | --- |
| `title` | required, `maxLength={120}` |
| `description` | `maxLength={5000}` |
| `cover` | `accept="image/png,image/jpeg"`, ≤ 5 MB, checked in `handleCoverChange` |
| `archive` | `accept=".zip,application/zip"` |

The page redirects to `/login` when there is no session:

```tsx
if (!user) {
  return <Navigate to="/login" replace state={{ from: location.pathname }} />;
}
```

The ZIP is **not** validated client-side: there is no size check and no `index.html` check.
Those errors only surface from the server as `400`.

`authService`/`gameUploadService` must not set `Content-Type` on the upload request, because
the browser has to add the `multipart/form-data` boundary itself:

```ts
await fetch(`${API_BASE_URL}/api/games`, {
  method: "POST",
  credentials: "include",
  headers: { [csrf.headerName]: csrf.token },
  body: formData,
});
```

## Preparing a Godot export

The three bundled games are Godot 4 HTML5 exports. To upload one:

1. In Godot: **Project → Export** → add the **Web** preset.
2. Tick `Export Project Path`, and use the default export filter so `.pck` and `.wasm` are
   emitted.
3. Export. Godot writes `index.html`, `index.js`, `index.pck`, `index.wasm` and the audio
   worklet files into the chosen folder.
4. Open that folder and select **all** the files — not the folder itself — and compress to
   ZIP.
5. In the upload form, attach that ZIP as `archive` and one of `index.png` or
   `icon.png` as `cover`.

> The `index.html` requirement is the one that catches people: ZIP the **contents** of the
> export folder, because a ZIP whose root contains `Blasteroids/index.html` is rejected.

## Related

- [Game Assets](Game-Assets.md) — how the files are stored and served
- [API](API.md#post-apigames) — the full endpoint reference
- [Database](Database.md#v2-create_games) — the `games` and `game_files` tables
- [Known Issues](Known-Issues.md) — memory usage and error-body inconsistencies