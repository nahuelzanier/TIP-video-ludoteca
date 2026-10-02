# Game Assets

How a browser game gets from an upload into a playable `<iframe>`.

## Where the files live

**In the database, not on disk.** Every file of every game is a row in `game_files`:

| Column | Value |
| --- | --- |
| `game_id` | the game's UUID |
| `path` | the path inside the game, e.g. `index.pck` |
| `content_type` | sniffed once at upload time |
| `content` | the bytes, as `bytea` |

The primary key is `(game_id, path)`, so serving an asset is one indexed lookup, and deleting
a game deletes its files via `ON DELETE CASCADE`. There is no games folder to provision,
synchronise or back up separately.

### What changed

Earlier versions of this project served files from folders in the repository:

```text
backend/gamesData/
  game1/
    index.html
    index.js
    index.pck
    blasteroids.wasm
    ...
```

with a `backend/src/main/resources/games.json` catalog and covers in
`frontend/public/images/`. **All of that is gone.** `git diff main featureComentarios` shows
31 files (~142.6 MB, mostly `.wasm` and `.pck`) deleted, plus `games.json` and the three
cover PNGs.

That history is still on the `main` branch, which is one reason merging is confusing. See
[Known Issues](Known-Issues.md#only-on-main-deleted-everywhere-else).

## The flow

```text
Godot HTML5 export folder
        |  select the contents, compress to ZIP
        v
POST /api/games  (cover + archive)
        |  GameUploadService: validate, unzip, insert
        v
games row (DRAFT)          + game_files rows, one per file
        |  POST /api/games/{id}/publish
        v
games row (PUBLISHED)
        |
        |  GET /games/{id}/index.html
        v
GameController  ->  game_files lookup  ->  bytes + content type
        |
        v
<iframe src="http://localhost:8080/games/{id}/index.html">
```

## A typical Godot 4 export

A Godot HTML5 export produces something like this. These are the real filenames from the three
games that used to live in `gamesData`:

```text
game1/  (Blasteroids)
  index.html                              <- the entry point, required
  index.js
  index.pck                               <- the game data, can be tens of MB
  blasteroids.wasm                        <- the engine, can be tens of MB
  blasteroids.audio.worklet.js            <- only if audio is enabled
  blasteroids.audio.position.worklet.js
  blasteroids.png                         <- good cover candidate
  blasteroids.icon.png
  blasteroids.apple-touch-icon.png

game3/  (LayLand) — the same shape, named after the project instead of "index"
  index.html
  LayLand.js
  LayLand.pck
  LayLand.wasm
  LayLand.audio.worklet.js
  LayLand.png
```

Note that Godot names the JS engine file after the project, but the **HTML entry point is
always `index.html`** — which is why that name is the one hard requirement.

## Requirements for an uploaded game

| Rule | Why |
| --- | --- |
| The ZIP must contain `index.html` **at its root** | the player requests `/games/{id}/index.html` by convention |
| Paths must be relative and free of `..` | traversal guard; absolute or `..` paths are rejected |
| Backslashes and colons are rejected | Windows-style separators and drive letters are refused |
| Max 2000 files | abuse guard |
| Max 75 MB per file, 100 MB extracted, 100 MB archive | zip-bomb and storage guard |
| No file named `__metadata__/thumbnail` | that path is reserved for the cover |
| Duplicate paths rejected | the `(game_id, path)` key cannot hold two versions of a file |

Full validation table with every message: [Game Upload](Game-Upload.md#validation-limits).

## How a file is served

`GameController` maps `/{gameId}/**` and does the lookup by hand:

```java
String requestPath = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);

String prefix = request.getContextPath() + "/games/" + gameId + "/";
if (!requestPath.startsWith(prefix)) {
    return ResponseEntity.badRequest().build();
}

String filePath = requestPath.substring(prefix.length());
Path normalized = Paths.get(filePath).normalize();

if (filePath.isBlank() || normalized.isAbsolute() || normalized.startsWith("..")) {
    return ResponseEntity.badRequest().build();
}

String databasePath = normalized.toString().replace("\\", "/");

return gameFiles.findById_GameIdAndId_Path(gameId, databasePath)
        .map(this::toResponse)
        .orElseGet(() -> ResponseEntity.notFound().build());
```

Because the lookup key contains the game id, a request scoped to one game physically cannot
return another game's row.

The response carries the content type recorded at upload and always sets:

```java
.header("X-Content-Type-Options", "nosniff")
```

which stops the browser from re-interpreting a stored file. The content type itself is derived
from the extension, with an explicit case for `.wasm`:

```java
if (lowerPath.endsWith(".html")) return "text/html; charset=UTF-8";
if (lowerPath.endsWith(".js"))   return "text/javascript; charset=UTF-8";
if (lowerPath.endsWith(".css"))  return "text/css; charset=UTF-8";
if (lowerPath.endsWith(".json")) return "application/json";
if (lowerPath.endsWith(".wasm")) return "application/wasm";
```

## The cover image

The cover is not a column on `games`. It is a normal `game_files` row at the reserved path
`__metadata__/thumbnail`, which lets `GameCoverController` reuse the same serving code:

```java
private static final String COVER_FILE_PATH = "__metadata__/thumbnail";
```

Its content type is decided by **magic bytes** rather than by the filename, so a file renamed
to `.exe` is still stored — and served — as `image/png`.

`games.cover_image_url` holds the API path, not a file path:

```text
/api/games/3f9c2a41-7b5e-4c8d-9e10-2a6b8c4d1f35/cover
```

The frontend turns it into an absolute URL:

```ts
image: new URL(game.image, API_BASE_URL).toString()
```

## The embed

`components/games/GameSection.tsx`:

```tsx
<section className="game-section">
  <iframe src={gameUrl} title="Juego" className="game-frame" />
</section>
```

CSS gives it `width: 100%`, `aspect-ratio: 16/9` and `border: none`. `Game.tsx` builds the
URL:

```tsx
const gameUrl = `${API_BASE_URL}/games/${game.id}/index.html`;
```

Because the frontend and the backend are different origins, the backend must allow the frame.
`SecurityConfig` handles both halves:

```java
.frameOptions(frame -> frame.disable())
.contentSecurityPolicy(csp -> csp
    .policyDirectives("frame-ancestors 'self' " + frontendOrigin))
```

So the game can only be embedded by the configured `FRONTEND_ORIGIN` or by itself. If the
iframe is blank, check the console for a CSP violation — see
[Getting Started](Getting-Started.md#a-game-does-not-load-or-the-iframe-is-blank).

> The iframe has **no `sandbox` attribute**, so an uploaded game runs with the backend's origin
> and can call any public endpoint. See
> [Known Issues](Known-Issues.md#security-notes).

## Drafts versus published games

Asset routes do **not** check the game status: `/games/{id}/index.html` serves a draft's files
if you know the UUID. What the status controls is discoverability — `GET /api/games`,
`/api/search` and the tag endpoints all filter on `PUBLISHED`, so a draft is invisible in the
UI but not unreachable by URL.

## Publishing a game

`GameService.publish` is the only transition, it is owner-only, and it is idempotent:

```java
if (!game.getOwner().getId().equals(user.getId())) {
    throw new GameEditForbiddenException();
}

game.publish();
games.save(game);
```

There is **no** unpublish and **no** edit. `Game` has no setters for `title` or `description`
— only `assignOwner(User)` and `publish()` mutate it. To change a game you would upload it
again, which creates a new `gameId`.

## Adding a new game

1. Export your game as a Godot HTML5 build, or produce any bundle with an `index.html` at its
   root.
2. Make sure every path inside is relative and free of `..`.
3. ZIP **the contents of the folder**, not the folder itself.
4. Upload the ZIP plus a PNG or JPEG cover at `/games/upload`.
5. Press "Publish game".

If you need to add files to an existing game from a script, insert `game_files` rows directly
and remember the `(game_id, path)` uniqueness plus the matching `content_type`.

## Related

- [Game Upload](Game-Upload.md) — the full upload and publish flow
- [API](API.md#game-files) — the two serving endpoints
- [Database](Database.md#v2-create_games) — the `games` and `game_files` tables
- [Frontend](Frontend.md) — the player component
- [Known Issues](Known-Issues.md) — the `main`-only binaries and the unsandboxed iframe