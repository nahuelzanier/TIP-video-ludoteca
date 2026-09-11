# Architecture

The application follows a simple split architecture: a React app for the storefront and a Spring Boot app for the catalog and static asset delivery.

## High-level flow

1. The frontend loads the game catalog from the backend API.
2. The home page renders the list of available games.
3. When the user selects a game, the route changes to `/game/:id`.
4. The frontend finds the selected item in the catalog and requests the corresponding game page from the backend.
5. The backend serves the static game files and the browser loads the web game.

## Frontend architecture

Relevant frontend files:

- `frontend/src/App.tsx` defines the routes.
- `frontend/src/pages/Home.tsx` renders the main catalog page.
- `frontend/src/pages/Game.tsx` loads the selected game by `id`.
- `frontend/src/services/gameService.ts` makes the API request.
- `frontend/src/types/Game.ts` defines the game model.

### Routing

```text
/            -> Home page
/game/:id    -> Selected game page
```

## Backend architecture

Relevant backend files:

- `backend/src/main/resources/games.json` contains the catalog metadata.
- `backend/src/main/java/com/tip_video_ludoteca/backend/controller/GameApiController.java` exposes the catalog through `/api/games`.
- `backend/src/main/java/com/tip_video_ludoteca/backend/controller/GameController.java` serves static game files from `backend/gamesData`.

## Data model

Each game is represented with the following fields:

```json
{
  "id": "game1",
  "title": "Blasteroids",
  "image": "/images/game1.png",
  "description": "Un clon de Asteroids."
}
```

This catalog is intentionally lightweight and easy to extend.

## Static assets flow

```text
game catalog JSON
   -> frontend selects game
   -> backend route: /games/{gameId}/index.html
   -> served from backend/gamesData/{gameId}/
```

This design is useful for browser-based games because each game can package its own HTML, JavaScript, audio worklets and PCK data without requiring a database or a custom upload process.
