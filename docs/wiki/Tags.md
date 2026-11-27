# Tags

Ludarium maintains a fixed catalog of supported tags. Users can add supported tags to
published games, and discussion creation can include forum tags. Arbitrary tag names are not
created through the user-facing API.

## API

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/tags` | public | List game tags |
| `GET` | `/api/games/{gameId}/tags` | public | List assigned tags and counts |
| `GET` | `/api/games/{gameId}/tags/mine` | session | Read caller's assignments |
| `POST` | `/api/games/{gameId}/tags` | session | Assign a supported tag |
| `GET` | `/api/forum/tags` | public | List tags available for discussions |

Game tag assignments are associated with both a game and the user who assigned them. The
database prevents duplicate assignments for the same user/game/tag combination. The fixed
catalog and assignment tables are introduced by Flyway; see [Database](Database.md#migrations).

See [API](API.md) for shared conventions and [Testing](Testing.md) for test commands.
