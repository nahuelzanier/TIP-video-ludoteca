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
