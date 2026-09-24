# Getting Started

This section explains how to run the app locally in development mode.

## Requirements

- Java 21
- Maven or Maven wrapper
- Node.js 18+
- npm

## Backend

From the project root:

```bash
cd backend
./mvnw spring-boot:run
```

The backend runs on:

```text
http://localhost:8080
```

The API endpoints are exposed under `/api` and the game asset routes are served under `/games`.

## Frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Then open:

```text
http://localhost:5173
```

## Useful verification endpoints

```bash
curl http://localhost:8080/api/games
```

This should return the game list from `backend/src/main/resources/games.json`.

## Typical development flow

1. Update the catalog in the backend JSON file.
2. Add or replace the corresponding game files in `backend/gamesData/<gameId>/`.
3. Refresh the frontend and test the game page.
4. Verify the game loads via the backend route: `http://localhost:8080/games/<gameId>/index.html`.

## Troubleshooting

- If the frontend cannot access the backend, check CORS and the `localhost:8080` URL.
- If a game does not load, verify that the `id` in the catalog matches the folder name under `backend/gamesData`.
- If there is a missing asset, confirm the file is present in the game directory and the route is correct.
