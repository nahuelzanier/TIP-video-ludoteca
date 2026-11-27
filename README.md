# Ludarium

Ludarium es una plataforma para compartir y jugar videojuegos HTML. Los autores suben juegos como borradores; administradores los prueban y publican.

## Stack

- Frontend: React 19, TypeScript, Vite, React Router y Vitest.
- Backend: Java 21, Spring Boot, Spring Security con sesiones y CSRF.
- Base de datos: PostgreSQL con migraciones Flyway.

## Funcionalidades

- Catálogo, búsqueda, recomendaciones y reproducción de juegos.
- Revisión administrativa de borradores antes de publicar.
- Personalización de juegos, comentarios, valoraciones y favoritos.
- Perfiles, settings persistentes, foros, discusiones, noticias y seguimiento de creadores.

## Desarrollo local

Requisitos: PostgreSQL 14+, Java 21 y Node.js 24. Configura DB_PASSWORD (o DEV_DB_PASSWORD en perfil dev).

```powershell
createdb -U postgres ludarium_dev
cd backend
$env:DEV_DB_PASSWORD = "tu-password-de-postgres"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

En otra terminal:

```powershell
cd frontend
npm ci
Set-Content .env.local "VITE_API_URL=http://localhost:8080`nVITE_GAME_CONTENT_URL=http://127.0.0.1:8080"
npm run dev
```

Abrir http://localhost:5173. La guía completa está en [Getting Started](docs/wiki/Getting-Started.md).

## Administración

Registra una cuenta normalmente. En desarrollo local, inicia sesión y abre `/admin/setup` con la clave `ludarium-local-admin-setup` para establecer el primer administrador. Después se pueden promover otras cuentas desde `/admin/review`, donde también se prueban y publican los borradores. Consulta [Authentication](docs/wiki/Authentication.md) y [Game Upload](docs/wiki/Game-Upload.md).

## Tests y CI

El backend usa Testcontainers (requiere Docker local): `cd backend; ./mvnw verify`. El frontend: `cd frontend; npm ci; npm run lint -- --max-warnings=0; npm run test:coverage; npm run build`. GitHub Actions corre ambos jobs; ver [Testing](docs/wiki/Testing.md).

## Documentación

La [wiki del repositorio](docs/wiki/Home.md) cubre arquitectura, API, base de datos, autenticación, frontend, settings, personalización, noticias, pruebas y limitaciones.

## Licencia

MIT. Ver [LICENSE](LICENSE).
