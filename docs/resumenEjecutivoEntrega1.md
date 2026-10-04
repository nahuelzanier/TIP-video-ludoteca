# Ludarium — Resumen de iteración (columna *Testing*)

---

## 1. Resumen ejecutivo

### 1.1 Qué se agregó / modificó en esta iteración

Las 6 cards en *Testing* (**34 puntos** según las etiquetas numéricas del tablero: 2 + 5 + 3 + 8 + 8 + 8) convierten la prueba de concepto del Sprint 1 en una aplicación con cuentas de usuario, contenido generado por la comunidad y búsqueda:

| User story | Puntos | Entró a *Testing* |
| --- | :---: | --- |
| Crear navbar | 2 | 22/09 |
| Inicio de sesión | 5 | 25/09 |
| Búsqueda | 3 | 28/09 |
| Perfil | 8 | 30/09 |
| Dejar comentarios y valoraciones | 8 | 01/10 |
| Subir juego | 8 | 02/10 |

Resultado a nivel producto: un usuario puede **registrarse, iniciar y cerrar sesión, subir un juego HTML5 (ZIP) y publicarlo, jugarlo en un reproductor embebido, comentarlo/valorarlo/responder, buscar juegos y usuarios, y ver perfiles públicos** con descripción editable.

Estado técnico de la rama:

- **Backend:** Java 21 + Spring Boot 4.1.1, Spring Security (sesiones + CSRF), Spring Data JPA, Flyway (migraciones V1–V7), PostgreSQL.
- **Frontend:** React 19 + TypeScript + Vite, `fetch` nativo, sin librerías de estado ni de componentes.
- **Tests:** 80 tests de backend en 9 clases (Mockito + `@WebMvcTest`). Frontend sin tests.
- **CI:** GitHub Actions corre `./mvnw test` y `lint` + `build` del frontend.

### 1.2 Decisiones tomadas

| Decisión | Motivo | Consecuencia / trade-off |
| --- | --- | --- |
| **Los archivos de cada juego pasan de carpetas del repo + `games.json` a filas `bytea` en PostgreSQL** (`game_files`) | Permite que cualquier usuario suba juegos sin tocar el repo ni el disco; un solo lookup indexado por `(game_id, path)`; `ON DELETE CASCADE` | Se eliminó el catálogo estático y ~142 MB de binarios Godot; cada lectura de asset es un acceso a base y una subida de 100 MB consume ~100 MB de heap |
| **Flujo de publicación en dos pasos** (`DRAFT` → `PUBLISHED`, solo el dueño publica) | El usuario puede subir y revisar antes de exponer el juego en el catálogo | La card original pedía "subir y ser redirigido a la página del juego"; hoy son dos botones en la misma pantalla |
| **Sesiones del lado del servidor + CSRF en lugar de JWT** | Catálogo y archivos son públicos, no hay consumidores de API de terceros; menos superficie de ataque en el navegador | El frontend debe enviar cookies (CORS con credenciales) y pedir el token CSRF antes de cada escritura |
| **Validación defensiva del ZIP** (límite de 100 MB comprimido/descomprimido, 75 MB por archivo, 2000 archivos, rutas normalizadas, `index.html` obligatorio en la raíz, cover validado por *magic bytes*) | El ZIP es la única entrada binaria no confiable de la app | Desafío técnico principal de *Subir juego*; hoy **no tiene tests** |
| **Comentarios con un solo nivel de respuestas** (una respuesta a una respuesta se aplana sobre el comentario raíz) | Coincide con el criterio de aceptación ("un poco más a la derecha") y evita árboles profundos | Los comentarios raíz se ordenan del más nuevo al más viejo; las respuestas, de la más vieja a la más nueva |
| **Búsqueda con `ILIKE`/`contains` + índices `pg_trgm` (migración V4)** | Resuelve búsqueda por nombre con costo bajo, sin motor externo | Solo busca por título de juego y nombre de usuario (ver desvíos) |
| **Navbar como menú lateral (drawer) con botón hamburguesa** | Una sola implementación responsive | Se aparta del mockup, que muestra una barra superior |
| **Replanificación de alcance** | Foro, selección aleatoria, recomendaciones y visuales siguen en *Backlog*; tags quedaron en una rama aparte (`feature/tags`, sin mergear); filtros y coverage ≥ 80 % siguen en *Spring actual* | Foro, Settings y Random Game existen como pantallas placeholder |
| **Stack HTML5/Godot; Flash descartado en la práctica** | Se investigó Flash en el Sprint 1 (card `investigar flash`) y todos los juegos de ejemplo son exports HTML5 de Godot | La card de *filtros* aún menciona "flash o html"; conviene actualizarla |

> **Riesgos a tener presentes antes de la demo:** `main` está desactualizada respecto de `featureComentarios` (sigue con el catálogo JSON y los binarios viejos); el CI no levanta PostgreSQL, por lo que `BackendApplicationTests` depende de una base local; el `iframe` del juego no está *sandboxed*. Detalle en `docs/wiki/Known-Issues.md`.

---

## 2. User stories implementadas

> Convención: ✅ cumple el criterio · ⚠️ cumple parcialmente / con desvío · ❌ no implementado en esta rama. El estado se verificó leyendo el código de `featureComentarios`; los criterios son los de las cards de Trello.

### US-1 · Navegación principal (navbar)

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:** menú de navegación accesible desde todas las pantallas que permite ir a las secciones principales, buscar y cerrar sesión.
- **Valor:** el usuario no se pierde: llega a cualquier parte de la app en un clic y tiene la búsqueda siempre a mano.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Barra de búsqueda | ✅ | Escribir ≥ 2 caracteres y enviar → navega a `/search?q=…` |
| Ir a la página principal | ✅ | Link *Home* → `/` |
| Ir al perfil | ✅ | Link *Profile* → `/profile`, que redirige a `/user/{username}` |
| Ir a configuración | ⚠️ | Link *Settings* funciona, pero la página es un placeholder |
| Botón para aleatorizar un juego | ⚠️ | Botón *Randomize game* navega a `/randomgame`, que está vacía (la card *selección aleatoria* sigue en Backlog) |
| Ir al foro | ⚠️ | Link *Forum* funciona, pero la página es un placeholder |
| Botón para cerrar sesión | ✅ | Llama a `POST /api/auth/logout` (204) y redirige a `/login` |

- **Mockup:** ver [`LudariumMU.png`](LudariumMU.png). Es el diseño de la home con barra superior (Explorar, Juegos, Devlogs, Comunidad, Subir juego, buscador, Iniciar sesión / Crear cuenta). La implementación actual es un menú lateral y aún no incorpora esa estética.

![Mockup Ludarium](LudariumMU.png)

---

### US-2 · Registro e inicio / cierre de sesión

- **Actores:** visitante (se registra e inicia sesión) y usuario registrado (cierra sesión).
- **Funcionalidad:** alta de cuenta con nombre de usuario, mail y contraseña; login y logout; las acciones que modifican contenido (comentar, subir y publicar juegos, editar la descripción del perfil) requieren sesión.
- **Valor:** identidad y autoría: cada comentario, juego y perfil pertenece a una persona, y el contenido está protegido frente a accesos anónimos.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Registro con perfil, mail y contraseña | ✅ | `/register`: usuario (3–30, `[A-Za-z0-9_]`), mail válido, contraseña 8–72 y confirmación. Duplicados devuelven `409` |
| Iniciar y cerrar sesión | ✅ | `/login` → `200` y cookie `JSESSIONID`; logout → `204` |
| Acciones bloqueadas sin sesión | ✅ | Escrituras sin sesión devuelven `401` (cubierto por `CommentApiControllerTests`, `ProfileApiControllerTests`, `GamePublishControllerTests`) |
| Desde login se puede ir a registro | ✅ | Link "Regístrate" en `/login` |
| Tras ingresar, volver a la página previa o a la principal | ⚠️ | Vuelve a la página previa cuando existe (`state.from`); **por defecto va a `/profile`, no a la página principal** |

- **Mockup:** no hay mockup específico; el mockup general muestra los botones *Iniciar sesión* y *Crear cuenta*.
- **Notas técnicas incluidas en la story:** hash de contraseñas con *delegating encoder* (bcrypt), rotación del id de sesión en cada login, token CSRF pedido antes de cada escritura, cookie `HttpOnly` y `SameSite=Lax`, sesión de 30 min.

---

### US-3 · Búsqueda

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:** buscar desde la navbar y ver una página de resultados con secciones separadas para juegos, foros y usuarios, paginadas.
- **Valor:** encontrar rápido el juego o creador que le interesa sin recorrer el catálogo.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Barra de búsqueda con icono en la navbar | ✅ | Ver US-1 |
| Buscar juegos por nombre | ✅ | `/search?q=blast` lista juegos cuyo título contiene el término (sin distinguir mayúsculas) |
| Buscar usuarios por nombre | ✅ | Sección *Usuarios* con link al perfil |
| Resultados con sección de foros | ⚠️ | La sección *Foros* existe pero muestra "disponible próximamente" |


- **Mockup:** buscador en la barra superior del [mockup](LudariumMU.png) ("Buscar juegos, tags o creadores…").
- **A verificar:** la consulta de juegos (`findByTitleContainingIgnoreCase`) no filtra por estado `PUBLISHED`, por lo que un juego en `DRAFT` podría aparecer en los resultados aunque el catálogo no lo muestre.

---

### US-4 · Comentarios y valoraciones de juegos

- **Actores:** usuario registrado (comenta, valora y responde); visitante (solo lee); autor del juego (su comentario se identifica).
- **Funcionalidad:** debajo de cada juego hay una sección de comentarios con caja de texto y botón *Subir*, valoración opcional de 1 a 5 estrellas y respuestas a comentarios.
- **Valor:** feedback directo a los creadores y conversación alrededor de cada juego, que es el núcleo social de Ludarium.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Sección de comentarios bajo cada juego | ✅ | `/game/:id` lista los comentarios; estado vacío "¡Sé el primero!" |
| Caja tipo YouTube con botón de subir | ✅ | Con sesión aparece el formulario (contenido máx. 1000 caracteres) |
| Muestra usuario y foto; tocarlos lleva al perfil | ✅ | Nombre y avatar enlazan a `/user/{username}`; la foto se sube con `PUT /api/users/{username}/avatar` (PNG/JPEG, 2 MB, validada por *magic bytes*) y si no hay se muestra la inicial |
| "(autor)" junto al nombre si comenta el autor | ✅ | `isGameAuthor` en la respuesta; cubierto por `CommentServiceTests` |
| Valoración opcional 1–5 visible junto al nombre | ✅ | Restricción `CHECK` en la base (1–5) y componente de estrellas; las respuestas no admiten rating |
| Responder comentarios, anidado y a la derecha | ✅ | Botón *Responder*; las respuestas se guardan bajo el comentario raíz con indentación |
| Sin sesión no se puede comentar | ⚠️ | Backend devuelve `401` y el frontend muestra "Iniciá sesión para comentar". La card *los comentarios requieren sesión* (redirigir a login) sigue en *Spring actual* |

- **Mockup:** no hay.
- **Cobertura de tests:** 15 tests de servicio y 9 de controlador (threading, validación, privacidad del listado y reglas de sesión).

---

### US-5 · Subir y publicar un juego

- **Actores:** usuario registrado (creador de juegos).
- **Funcionalidad:** formulario para subir un juego HTML5 como ZIP con título, descripción y portada; el juego queda en borrador y, al publicarlo, aparece en el catálogo y es jugable en un `iframe`.
- **Valor:** es la propuesta central de la plataforma: que cualquier desarrollador distribuya sus juegos sin intermediarios.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Botón *añadir juego* visible solo con sesión | ⚠️ | Existe como *Subir un juego* en **el perfil propio**, no en la página principal. `/games/upload` redirige a `/login` si no hay sesión |
| Campos: nombre, portada, descripción | ✅ | Título (≤ 120), descripción (≤ 5000), portada PNG/JPEG (≤ 5 MB), ZIP (≤ 100 MB con `index.html` en la raíz) |
| Campos: tags, cantidad de jugadores, tecnologías | ❌ | No están en esta rama (tags en `feature/tags`) |
| Al subir, persistir en la BD y mostrarlo en la principal | ✅ | `POST /api/games` → `201` en `DRAFT`; `POST /api/games/{id}/publish` → aparece en `GET /api/games` |
| Luego redirigir a la página del juego | ⚠️ | No redirige: muestra un mensaje de éxito y el usuario publica con un segundo botón |
| Solo el dueño puede publicar | ✅ | `403` para otros usuarios (`GamePublishControllerTests`) |

- **Mockup:** el [mockup](LudariumMU.png) incluye el acceso *Subir juego* en la navbar y *Subir tu juego* en el hero; no hay mockup del formulario.
- **Desafío técnico:** servir cada archivo del juego desde la base con el `Content-Type` correcto (incluido `.wasm`) y permitir el `iframe` entre orígenes (`frame-ancestors` restringido al frontend).

---

### US-6 · Perfil de usuario

- **Actores:** cualquier visitante (ve perfiles); usuario registrado (edita su propio perfil).
- **Funcionalidad:** página pública `/user/{username}` con nombre, descripción editable en un modal (máx. 500 caracteres) y sección *Mi actividad*.
- **Valor:** identidad pública del creador; permite llegar al autor desde un comentario o una búsqueda.
- **Criterios de aceptación y validación:**

| Criterio (Trello) | Estado | Cómo se valida |
| --- | :---: | --- |
| Nombre de usuario y foto de perfil | ✅ | Nombre y foto (`avatarUrl` derivado de los bytes); sin foto se cae a la inicial |
| Descripción opcional editable con un botón | ✅ | *Editar descripción* abre el modal; solo el dueño (`403` para otros; `ProfileApiControllerTests`) |
| Sección *Mis juegos* (oculta si no hay) | ✅ | `GET /api/users/{username}/games`; la sección se titula *Mis juegos* en el perfil propio y *Juegos publicados* en el ajeno, y no se monta si no hay juegos |
| Sección *Mi actividad* con foros creados | ⚠️ | La sección existe pero está vacía hasta que exista el foro |

- **Mockup:** no hay.
- **Privacidad:** el endpoint público del perfil no expone el email.

