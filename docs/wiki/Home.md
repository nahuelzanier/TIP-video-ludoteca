# Ludarium Wiki

Ludarium is a web project for browsing, playing and presenting indie games in a catalog-style interface similar to itch.io. The project is currently in development and combines a React frontend with a Spring Boot backend to serve the game catalog and the static game assets.

## Overview

- Frontend: React + TypeScript + Vite
- Backend: Java + Spring Boot
- Main purpose: display a list of games, open a game page and load each game from a dedicated folder
- Content model: a JSON catalog that describes each game and the backend serves the corresponding static files

## Main screens

- Home page: shows all available games in a grid
- Game page: displays the selected title and loads the game HTML in an embedded player area

## Stack

### Frontend
- React 19
- Vite
- React Router
- TypeScript

### Backend
- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Static resource exposure for game folders

## Project structure

```text
backend/
  gamesData/
    game1/
    game2/
    game3/
  src/main/java/com/tip_video_ludoteca/backend/
  src/main/resources/games.json
frontend/
  src/
  public/
  package.json
README.md
```

## Quick links

- [Getting Started](Getting-Started.md)
- [Architecture](Architecture.md)
- [API](API.md)
- [Game Assets](Game-Assets.md)

## Contributors

- Nahuel Zanier
- Lucas Sanguinetti
