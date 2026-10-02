Ludarium es una pagina web donde distintos desarrolladores de videojuegos pueden compartir, jugar, y hablar sobre el mundo de los videojuegos.
Todos pueden jugar juegos que hayan subido otras personas, opinar sobre los mismos, buscarlos y tambien subirlos dentro de nuestro ecosistema

integrantes: Nahuel Zanier, Lucas Sanguinetti

## Stack

- **Frontend:** React 19 + TypeScript + Vite (sin librerias de estado ni de componentes, `fetch` nativo)
- **Backend:** Java 21 + Spring Boot 4.1.1 (Spring Security con sesiones y CSRF, Spring Data JPA)
- **Base de datos:** PostgreSQL, esquema manejado con Flyway. Los archivos de cada juego se guardan como `bytea` en la tabla `game_files`.

## Arranque rápido

Requiere **PostgreSQL**, **Java 21** y **Node.js**. El backend no arranca sin `DB_PASSWORD`.

```bash
createdb -U postgres ludarium

cd backend
export DB_PASSWORD="tu-password-de-postgres"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

```bash
cd frontend
npm install
printf 'VITE_API_URL=http://localhost:8080\n' > .env.local
npm run dev
```

Abrir `http://localhost:5173`. El perfil `dev` crea el usuario `lucas2` / `lucas222` y publica los juegos en `DRAFT`.

Los detalles, incluida la migracion `pg_trgm` y el troubleshooting, estan en [Getting Started](docs/wiki/Getting-Started.md).

## Tests

```bash
cd backend && ./mvnw test      # 8 clases, 56 tests
cd frontend && npm run lint && npm run build
```

El frontend todavia no tiene tests. Ver [Testing](docs/wiki/Testing.md).

## Documentación

La documentación del proyecto está organizada como un wiki estilo GitHub en la carpeta `docs/wiki`.

- [Home](docs/wiki/Home.md)
- [Getting Started](docs/wiki/Getting-Started.md)
- [Architecture](docs/wiki/Architecture.md)
- [Database](docs/wiki/Database.md)
- [API](docs/wiki/API.md)
- [Authentication](docs/wiki/Authentication.md)
- [Game Upload](docs/wiki/Game-Upload.md)
- [Game Assets](docs/wiki/Game-Assets.md)
- [Frontend](docs/wiki/Frontend.md)
- [Testing](docs/wiki/Testing.md)
- [Tags](docs/wiki/Tags.md) — en la rama `feature/tags`, todavia no mergeada
- [Known Issues](docs/wiki/Known-Issues.md) — bugs y deuda tecnica

## Estado del proyecto

`main` esta atrasada con respecto a las ramas de trabajo y todavia contiene el catalogo JSON viejo (`games.json`) junto con ~142 MB de binarios de Godot en `backend/gamesData/`, que el codigo actual ya no usa. El codigo vigente esta en `featureComentarios`. Ver [Known Issues](docs/wiki/Known-Issues.md#branch-divergence).

No hay configuracion de despliegue en el repositorio: no hay Dockerfile, ni compose, ni perfil de Spring para produccion.

## WIKI
- https://github.com/nahuelzanier/TIP-video-ludoteca/wiki

## Licencia

Este proyecto está licenciado bajo la licencia MIT. Ver [LICENSE](LICENSE).