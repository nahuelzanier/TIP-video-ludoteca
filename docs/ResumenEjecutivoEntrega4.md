# Ludarium: resumen ejecutivo de la Entrega 4

**Tablero:** Proyecto videojuegos (Trello)  
**Fecha del resumen:** 27/11/2026  
**Período cubierto:** 14/11 al 26/11/2026  
**Alcance:** funcionalidades incorporadas a `dev` después de la Entrega 3: preferencias y cuenta, personalización de páginas de juego, y noticias con seguimiento de creadores.

**Actores:**

- **Visitante:** persona sin sesión iniciada; puede consultar perfiles y noticias públicas.
- **Usuario registrado:** gestiona su cuenta, sigue creadores, lee su feed y comenta.
- **Autor de juegos:** usuario registrado que personaliza la ficha de sus juegos publicados.
- **Creador de noticias:** usuario registrado que publica y administra sus propias noticias.

---

## Resumen ejecutivo

### Qué se agregó o modificó en esta entrega

Sobre la plataforma de juegos y foros de la Entrega 3, esta entrega incorpora:

- **Configuración de usuario:** preferencias persistentes de volumen, idioma y tema; cambio de nombre de usuario y contraseña; y cierre de sesión desde Settings.
- **Personalización de páginas de juego:** el autor puede ajustar fondo, colores, tipografía y estilo de fondo de la ficha. La personalización queda asociada al juego y se muestra públicamente.
- **Noticias y seguimiento:** los usuarios pueden seguir creadores, consultar un feed paginado con sus noticias, y publicar, editar o eliminar sus propias noticias.
- **Conversación sobre noticias:** las noticias admiten comentarios y respuestas, reutilizando el sistema de comentarios existente sin valoraciones.
- **Persistencia y cobertura:** se agregaron migraciones para las nuevas relaciones y pruebas de servicios, controladores, API frontend y persistencia.

**Esfuerzo estimado:** los puntos de las tarjetas de Trello no están incluidos en el código consultado; deben confirmarse con las etiquetas del tablero.

### Decisiones y desafíos

1. **Se evitó duplicar datos de cuenta.** Nombre de usuario, correo y contraseña siguen en `app_user`; las preferencias adicionales se guardan en una tabla `user_settings` uno a uno con la cuenta.
2. **La personalización pertenece al juego.** Se guarda por `game_id` con clave foránea y eliminación en cascada. La autorización compara el ID estable del dueño, por lo que un cambio de nombre de usuario no transfiere ni elimina permisos.
3. **El feed se construye a partir de relaciones de seguimiento.** Solo requiere iniciar sesión para consultar el feed; las noticias individuales pueden leerse públicamente. No se permite seguirse a uno mismo.
4. **Los comentarios se generalizaron para juegos y noticias.** Se añadió un tipo de destino y una relación específica para noticias, manteniendo ratings solo para los juegos.
5. **El guardado de ejecución (*save state*) no forma parte de esta entrega.** El navegador no ofrece una captura universal del runtime de un juego HTML; ese trabajo requeriría soporte explícito del motor o del juego.

---

## User stories

### US-01: Administrar mis preferencias y mi cuenta

- **Actores:** usuario registrado.
- **Funcionalidad:** una página de Settings permite guardar volumen, idioma y tema; actualizar el nombre de usuario y la contraseña; y cerrar sesión.
- **Valor:** el usuario administra su cuenta desde un solo lugar y conserva sus preferencias al iniciar sesión en otro equipo.
- **Criterios de aceptación:**
  - Las preferencias se leen y guardan en una tabla asociada a la cuenta.
  - El cambio de usuario actualiza el registro existente, sin crear datos redundantes.
  - El cambio de contraseña verifica la contraseña actual y guarda la nueva contraseña con hash.
  - La interfaz valida la confirmación de contraseña y muestra errores o confirmaciones.
  - El cierre de sesión finaliza la sesión del servidor.
- **Persistencia:** migración `V15__create_user_settings.sql`; los datos de identidad permanecen en `app_user`.

### US-02: Personalizar la página de mi juego

- **Actores:** autor de juegos y visitantes.
- **Funcionalidad:** el dueño de un juego publicado puede personalizar el fondo, el color de acento, el color del texto, la fuente y el estilo sólido o degradado. Los cambios se previsualizan antes de guardarlos.
- **Valor:** cada creador puede darle una identidad visual propia a la página de su juego, que también ven sus visitantes.
- **Criterios de aceptación:**
  - Solo el dueño puede guardar cambios; la API valida la autorización con el ID de cuenta.
  - Los colores se validan como valores hexadecimales y las fuentes/estilos se restringen a opciones admitidas.
  - La configuración se guarda por juego y se devuelve al consultar su detalle.
  - Al borrar el juego, se elimina también su personalización.
- **Opciones incluidas:** fuente predeterminada, Arial, Verdana, Trebuchet MS, Georgia y Courier New; fondo sólido o degradado.
- **Persistencia:** tabla `game_customizations`, migraciones `V16` y `V17`.

### US-03: Seguir creadores y leer sus noticias

- **Actores:** visitante, usuario registrado y creador de noticias.
- **Funcionalidad:** los usuarios registrados pueden seguir o dejar de seguir perfiles. El feed muestra noticias de los creadores seguidos; cada noticia tiene detalle público, imagen opcional, comentarios y respuestas.
- **Valor:** los usuarios reciben novedades de creadores que les interesan y los autores pueden compartir avances con su comunidad.
- **Criterios de aceptación:**
  - Seguir y dejar de seguir requiere sesión; no se permite seguir la propia cuenta.
  - El perfil informa cantidades de seguidores y seguidos, y el estado de seguimiento del usuario actual.
  - El feed es paginado y muestra únicamente noticias de creadores seguidos.
  - Un usuario puede crear noticias y editar o eliminar únicamente las propias.
  - La imagen es opcional, se valida por contenido y no puede superar 5 MB.
  - Cualquier visitante puede leer el detalle; para comentar o responder necesita sesión.
  - Las noticias no admiten valoraciones; los comentarios conservan la autoría y las respuestas existentes.
- **Persistencia:** tablas `follows`, `noticias` e `noticia_images`; migraciones `V18`–`V20`.

---

## Calidad y CI

- Se añadieron pruebas para servicios y controladores de noticias y seguimiento, además de pruebas para Settings y personalización de juegos.
- El frontend mantiene los pasos de CI de lint, cobertura y build.
- El backend ejecuta `./mvnw verify` y usa PostgreSQL mediante Testcontainers; por lo tanto, el job necesita Docker disponible.
- La personalización tiene cobertura de validación HTTP, autorización, manejo del ID del juego y persistencia relacional.

---

## Puntos a revisar antes del cierre

- **Volumen:** la preferencia se persiste, pero no puede aplicarse automáticamente a todos los juegos HTML; cada juego tendría que exponer control de audio o integrarse con una API de Ludarium.
- **Idioma:** la traducción se aplica a las pantallas conectadas al sistema i18n; todavía puede haber textos literales sin traducir en otras partes de la interfaz.
- **Formato de imágenes:** el formulario de noticias acepta PNG, JPEG y WebP, mientras que el validador backend actual admite PNG y JPEG. Conviene alinear ambos antes de dar soporte por terminado.
- **CI completo:** la suite backend necesita Docker/Testcontainers. La ejecución local sin Docker no permite confirmar las pruebas de persistencia; el estado final debe verificarse en el job de GitHub Actions.
- **Save states:** quedó fuera del alcance. Para juegos HTML no existe una captura genérica del estado de ejecución; se requeriría soporte explícito de cada juego o motor.
