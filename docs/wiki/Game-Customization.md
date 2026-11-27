# Game page customization

Each game can have one appearance customization, edited by its owner from the game page. The
customization affects Ludarium's game presentation (including title and surrounding page
text); it does not rewrite the uploaded game files.

## Options

| Field | Values |
| --- | --- |
| Background color | Hex color |
| Accent color | Hex color |
| Text color | Hex color |
| Background style | `SOLID` or `GRADIENT` |
| Font | System, serif, monospace, Arial, Verdana, Trebuchet, Georgia or Courier |

## API and authorization

| Method | Path | Access |
| --- | --- | --- |
| `GET` | `/api/games/{gameId}/customization` | public |
| `PUT` | `/api/games/{gameId}/customization` | signed-in game owner |

The backend identifies the owner using the stable user ID associated with the game, not the
editable username. A customization has a one-to-one relationship with a game and is deleted
when that game is deleted. See migration V16 and its follow-up V17 in
[Database](Database.md#migrations).
