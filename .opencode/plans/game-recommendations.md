# Plan: Sección "Recomendaciones" en la página de detalle de juego

## Contexto descubierto (hallazgos clave)

- **Tags NO son ManyToMany en `Game`.** El modelo usa la entidad join explícita `GameTagAssignment` (`tags/GameTagAssignment.java`) con `@EmbeddedId GameTagAssignmentId (game_id, tag_id, user_id)`. Un juego "tiene" un tag cuando ≥1 usuario lo asignó; varios usuarios pueden asignar el mismo tag al mismo juego → el cómparto debe contar `COUNT(DISTINCT tag_id)`, no `COUNT(*)`.
- `Game` (`games/Game.java`) tiene `id (String)`, `title`, `coverImageUrl`, `status (DRAFT|PUBLISHED)`; sin relación a tags.
- Endpoint existente más parecido: `GET /api/games/{gameId}` en `controller/GameApiController.java` (DTO record anidado `GameSummary(id,title,image,description)`). Errores: `GameNotFoundException` → `GlobalExceptionHandler` → 404 `{"error":"El juego no existe."}`.
- Seguridad: `SecurityConfig` ya hace `permitAll` para GET `/api/games/**` → el endpoint nuevo en esa ruta es público sin tocar config.
- Frontend: página `frontend/src/pages/Game.tsx` compone secciones (`GameSection`, `GameTagSection`, `CommentSection`), cada una recibe `gameId`. Fetch con `fetch` nativo en `src/services/*`, base `API_BASE_URL`, rebasing de imagen con `new URL(image, API_BASE_URL)`. Estilos: CSS plano por componente con tokens `var(--*)`, grid responsivo 3/2/1 cols en `GameGrid.css`. Ruta de juego: `/game/:id` con `<Link to={...}>`.
- Fallback de imágenes: no existe placeholder para covers de juegos; el patrón de la app es el de avatares (letra inicial + `onError`). Reusar ese patrón.
- Tests backend: JUnit5 + AssertJ; `@DataJpaTest` con Postgres real (`Replace.NONE`, ej. `games/GameRepositoryTests.java`) y `@WebMvcTest` + `@MockitoBean` (ej. `controller/GameApiControllerTests.java`). **No hay tests de frontend** (sin framework de testing instalado) → no se agrega ninguno (el pedido era condicional: "si el proyecto ya tiene tests de frontend").
- Rama actual: `feature_Juegos_Aleatorios`, árbol limpio.

## Decisiones (a reportar al final)

1. **Estado vacío: no se muestra la sección** si no hay recomendaciones (ni loading ni error rompen la página: se renderiza `null`). Es lo más discreto y coherente con la página de detalle.
2. **Desempate determinista:** shared-tags DESC, luego `title ASC`, luego `id ASC`.
3. **Endpoint:** `GET /api/games/{gameId}/recommendations` (público automáticamente por `SecurityConfig`).
4. Solo juegos **PUBLISHED** son candidatos (y el juego actual debe ser PUBLISHED, si no → 404 `GameNotFoundException`, mismo envelope `{"error":...}` que el resto de la API — decidido sobre el `ResponseEntity.notFound().build()` sin body que hoy tiene `getGame`, porque el estilo documentado de la API es el envelope).
5. Se reutiliza el patrón de letra-inicial de avatar como placeholder de cover faltante/rota.

## Cambios backend

### 1. NUEVO `backend/src/main/java/com/tip_video_ludoteca/games/GameRecommendationSummary.java`
Record liviano para el DTO y la expresión de constructor JPQL:
```java
public record GameRecommendationSummary(String id, String title, String image) {}
```

### 2. MODIFICAR `backend/src/main/java/com/tip_video_ludoteca/tags/GameTagAssignmentRepository.java`
Agregar query JPQL (misma forma que `findTagSummariesByGameId`):
```java
@Query("""
        SELECT new com.tip_video_ludoteca.games.GameRecommendationSummary(
            game.id, game.title, game.coverImageUrl)
        FROM GameTagAssignment assignment
        JOIN assignment.game game
        WHERE assignment.id.gameId <> :gameId
          AND game.status = com.tip_video_ludoteca.games.GameStatus.PUBLISHED
          AND assignment.id.tagId IN (
              SELECT other.id.tagId FROM GameTagAssignment other
              WHERE other.id.gameId = :gameId)
        GROUP BY game.id, game.title, game.coverImageUrl
        HAVING COUNT(DISTINCT assignment.id.tagId) >= 2
        ORDER BY COUNT(DISTINCT assignment.id.tagId) DESC,
                 game.title ASC,
                 game.id ASC
        """)
List<GameRecommendationSummary> findRecommendationsForGame(
        @Param("gameId") String gameId, Pageable pageable);
```
Se llama con `PageRequest.of(0, 3)` (LIMIT 3 en DB). `COUNT(DISTINCT tagId)` neutraliza asignaciones duplicadas por múltiples usuarios. La exclusión del propio juego y el filtro a PUBLISHED están en la query.

### 3. MODIFICAR `backend/src/main/java/com/tip_video_ludoteca/controller/GameApiController.java`
- Inyectar también `GameTagAssignmentRepository`.
- Nuevo endpoint (siguiendo el estilo del resto de la clase):
```java
@GetMapping("/{gameId}/recommendations")
public List<GameRecommendationSummary> getGameRecommendations(@PathVariable String gameId) {
    Game game = games.findById(gameId).orElse(null);
    if (game == null || game.getStatus() != GameStatus.PUBLISHED) {
        throw new GameNotFoundException();
    }
    return tagAssignments.findRecommendationsForGame(gameId, PageRequest.of(0, 3));
}
```
- `GameNotFoundException` ya está manejada en `GlobalExceptionHandler` (404 + `{"error":"El juego no existe."}`). No hace falta tocar excepciones ni security.

### 4. Tests backend

**NUEVO `backend/src/test/java/com/tip_video_ludoteca/tags/GameTagAssignmentRepositoryTests.java`** — `@DataJpaTest` + `Replace.NONE` (patrón de `GameRepositoryTests`), usando los 15 tags sembrados por Flyway V6 vía `TagRepository.findBySlug(...)`. `@BeforeEach` limpia games (cascade ON DELETE CASCADE limpia assignments). Casos:
- 0 tags compartidos → lista vacía.
- 1 tag compartido → lista vacía (no llega al umbral).
- 2+ tags compartidos → incluye al juego.
- Más de 3 candidatos → devuelve exactamente 3, ordenados por shared DESC.
- Exclusión del propio juego (un juego con 5 tags compartidos consigo mismo no aparece).
- Empates → orden determinista por título (crear candidatos con igual nº de tags y títulos a/b/c → esperar orden alfabético).
- (Implícito) candidato solo DRAFT no aparece.

**MODIFICAR `backend/src/test/java/com/tip_video_ludoteca/controller/GameApiControllerTests.java`**
- Agregar `@MockitoBean GameTagAssignmentRepository gameTagAssignmentRepository`.
- Tests nuevos: 200 con lista de `GameRecommendationSummary` (jsonPath id/title/image); 404 + `$.error == "El juego no existe."` para id inexistente.

## Cambios frontend

### 5. MODIFICAR `frontend/src/types/Game.ts`
Agregar:
```ts
export interface GameRecommendation {
  id: string;
  title: string;
  image: string;
}
```

### 6. MODIFICAR `frontend/src/services/gameService.ts`
```ts
export async function getGameRecommendations(gameId: string): Promise<GameRecommendation[]> {
  const response = await fetch(
    `${API_BASE_URL}/api/games/${encodeURIComponent(gameId)}/recommendations`,
  );
  if (!response.ok) {
    throw new Error(
      response.status === 404
        ? "Game not found."
        : "Could not load recommendations.",
    );
  }
  const recommendations: GameRecommendation[] = await response.json();
  return recommendations.map((game) => ({
    ...game,
    image: game.image ? new URL(game.image, API_BASE_URL).toString() : "",
  }));
}
```
(guard contra `image: null` del backend; el resto sigue el patrón de `getGameById`).

### 7. NUEVO `frontend/src/components/games/GameRecommendations.tsx`
- Props: `{ gameId: string }`.
- `useState` loading/error/items + `useEffect` con dep `[gameId]` y flag de cancelación `isCurrent` (patrón idéntico a `GameTagSection`).
- Render: `null` si `loading`, si `error`, o si `items.length === 0` (decisión 1).
- Si hay items: `<section className="game-recommendations">` con `<h2>Recomendaciones</h2>` y un grid de tarjetas; cada tarjeta es `<Link className="game-recommendation-card" to={`/game/${game.id}`}>` con `<img>` + `<h3>{title}</h3>` (solo imagen y título).
- Placeholder de imagen: si `!game.image` o `onError`, mostrar un `div` con la inicial del título (patrón de `SearchUserAvatar`).

### 8. NUEVO `frontend/src/components/games/GameRecommendations.css`
- Grid responsivo 3/2/1 columnas (mismos breakpoints 900/600 de `GameGrid.css`).
- Tarjeta con `var(--surface)`, `var(--border)`, `border-radius`, hover con `var(--accent)` (imitando `GameCard.css`), sin descripción.

### 9. MODIFICAR `frontend/src/pages/Game.tsx`
- Importar `GameRecommendations` y agregar `<GameRecommendations gameId={game.id} />` después de `<CommentSection />` (al final de la página).
- Al navegar entre recomendaciones: `Game.tsx` ya refetchea cuando cambia `id` (su `useEffect` depende de `[id]`), y `GameRecommendations` recibe el `gameId` nuevo como prop con effect que depende de `[gameId]` → se actualiza correctamente.

## Verificación
- Backend: `./mvnw test` (en `backend/`). Nota: los `@DataJpaTest` requieren Postgres local corriendo (mismo caso que los tests existentes).
- Frontend: `npm run lint` y `npm run build` (en `frontend/`). No hay framework de tests de frontend instalado, así que no se agrega test de componente.

## Fuera de alcance
- No se tocan: modelo de tags, `SecurityConfig`, docs del wiki, otras páginas.
