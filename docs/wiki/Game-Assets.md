# Game Assets

Each game is stored in its own folder under the backend game data directory. This is how the project packages and serves browser games.

## Directory structure

```text
backend/
  gamesData/
    game1/
      index.html
      index.js
      index.pck
      index.audio.worklet.js
      index.audio.position.worklet.js
    game2/
      homeNoLonger.html
      homeNoLonger.js
      homeNoLonger.pck
      homeNoLonger.audio.worklet.js
      homeNoLonger.audio.position.worklet.js
    game3/
      LayLand.html
      LayLand.js
      LayLand.pck
      LayLand.audio.worklet.js
```

## How a game is served

The game metadata points to the selected game id, and the frontend builds a URL like this:

```text
http://localhost:8080/games/{gameId}/index.html
```

The backend then resolves the request to the game folder and returns the corresponding static file. This keeps each game self-contained.

## Adding a new game

1. Create a new directory under `backend/gamesData/<gameId>/`.
2. Place the HTML, JS, audio and other required files there.
3. Add the game object to `backend/src/main/resources/games.json`.
4. Ensure the `id` matches the folder name exactly.
5. Verify that the browser can open the game from the route.

## Recommended conventions

- Use a unique game id such as `game4`, `platformer1`, or `maze-runner`.
- Keep the entry file named `index.html` when possible.
- Keep assets local to the game folder to avoid collisions.
- Keep the metadata short and clear: title, description and image.

## Notes

The current setup is optimized for simple static web game bundles and does not yet include user accounts, game uploads, moderation or a database-backed CMS.
