# Ludarium: resumen ejecutivo de lo que está en Testing (iteración 2)

**Tablero:** Proyecto videojuegos (Trello)
**Fecha del resumen:** 13/11/2026
**Período cubierto:** 30/10 al 13/11/2026
**Alcance:** las 20 tarjetas actuales de la columna *Testing*, agrupadas en 6 user stories.

**Actores:**

- **Visitante:** persona sin sesión iniciada.
- **Usuario registrado:** persona con sesión iniciada.
- **Autor de juegos:** usuario registrado que sube juegos HTML.
- **Equipo de desarrollo:** responsables de la calidad, los datos de prueba y la seguridad de la plataforma.

---

## Resumen ejecutivo

### Qué se agregó o modificó en esta iteración

La iteración anterior se cerró el 30 y 31 de octubre con 10 historias en *Sprint 3 Done*: navegación, filtros, aleatorio, recomendaciones, redirección al login, comentarios con sesión, foro (listado inicial y creación), visuales de perfil y cobertura de tests. Sobre esa base, esta iteración suma:

- **Foro completo:** listado, detalle con respuestas, creación asociada a un juego, valoraciones, tags propios del foro, buscador y filtros.
- **Juegos favoritos:** guardar favoritos, página "Mis favoritos" y persistencia visual del icono.
- **Identidad y autoría:** imagen de perfil en la navbar, creador visible en cada juego y foros publicados en el perfil.
- **Control de publicación y seguridad de datos:** flujo de publicación de juegos, bloqueo del acceso público a borradores y seeder seguro.
- **Pulido de interfaz:** nombre Ludarium en la página principal, todo el UI en inglés, filtros desplegables y mejoras visuales generales.

**Esfuerzo estimado en Testing:** 43 puntos, según las etiquetas numéricas de las tarjetas.



### Decisiones y desafíos

1. **Se retomó la deuda de seguridad y datos con prioridad alta.** Tres tarjetas de prioridad alta habían vuelto al Backlog el 17/10 y se retomaron el 30/10: impedir el acceso público a borradores, el seeder que pisaba juegos existentes y el flujo de publicación. Las tres están ahora en Testing.
2. **Las dos tarjetas de seguridad de la iteración anterior no avanzaron.** `sandbox` del iframe y revisión de CSRF volvieron de Testing al Backlog el 30/10, sin comentarios que expliquen el motivo. El aislamiento de origen en producción también sigue en Backlog. El riesgo original (juegos de terceros ejecutándose con acceso a la sesión del usuario) sigue sin resolverse del todo.
3. **El foro se completó en el orden pendiente.** Lo que se había recortado antes (detalle, respuestas, valoraciones) se resolvió ahora. Además se agregaron tags propios del foro (24 categorías, como Discusión, Reseña, Bug o Guía), independientes de los tags de los juegos, con migración de base de datos.
4. **Se definió un único idioma para la interfaz.** Había mezcla de español e inglés, y se decidió estandarizar todo en inglés. La internacionalización quedó como tarjeta vacía en el Backlog, o sea, un posible paso futuro y no parte de esta iteración.
5. **Aparecieron funcionalidades nuevas no planeadas.** Los favoritos no estaban en el tablero anterior. También se cerraron tarjetas de ajuste ("mover icono favorito al texto de la card", "definir el tema de draft"), lo que sugiere decisiones tomadas probando lo ya construido.
6. **Quedaron fuera de la iteración:** la página de Settings (con modo oscuro) y la personalización de la página del juego (color y fuente), ambas en Backlog.

---

## User stories

### US-01: Explorar y participar en las discusiones del foro

- **Actores:** visitante (lee) y usuario registrado (crea, responde y valora).
- **Funcionalidad:**
  - Listado de discusiones.
  - Página de detalle con respuestas.
  - Creación de una discusión asociada a un juego publicado.
  - Valoración de discusiones de 1 a 5.
  - Icono de acceso al foro desde el home.
- **Valor:** convierte el foro en un espacio real de conversación sobre los juegos y le da a la comunidad una forma de opinar y valorar.
- **Criterios de aceptación:**
  - **Listado:**
    - Cada tarjeta muestra título, autor, avatar, descripción breve, portada y tags.
    - Se pueden abrir las páginas de las discusiones.
    - El listado maneja carga, errores y estado vacío, y tiene paginación.
  - **Detalle y creación:**
    - El detalle muestra la información de la discusión y sus respuestas.
    - Usuarios autenticados pueden responder; usuarios sin sesión solo pueden leer.
    - La creación permite elegir juego, título, descripción, portada y tags.
    - La aplicación muestra errores de validación y confirma la creación.
    - Las rutas están integradas en `App.tsx`.
  - **Valoraciones:**
    - Un usuario autenticado puede valorar una discusión del 1 al 5.
    - Cada usuario tiene una sola valoración por discusión y puede actualizarla.
    - El promedio y la cantidad de valoraciones se actualizan correctamente.
    - Los usuarios sin sesión pueden consultar el promedio, pero no valorar.
  - **Home:** la lista de foros del home muestra un icono (la tarjeta no tiene descripción; criterio derivado del título).
- **Mockup:** no hay mockup adjunto.
- **Tarjetas incluidas:** `Crear la página de listado de foros`, `Crear las páginas de detalle y creación de discusiones`, `Implementar valoraciones de discusiones`, `Agregar icono a la lista de foros en el home`.

### US-02: Clasificar y encontrar discusiones

- **Actores:** visitante y usuario registrado (buscan y filtran); usuario registrado (asigna tags al crear).
- **Funcionalidad:** cada discusión puede llevar tags propios del foro, elegidos de un catálogo fijo. El listado `/forum` tiene un buscador y filtros por esos tags.
- **Valor:** a medida que crece el foro, permite llegar rápido a lo que se busca (una guía, un bug, una reseña).
- **Criterios de aceptación:**
  - **Tags:**
    - Existe un catálogo de tags del foro, separado de los tags de juegos.
    - Cada discusión puede tener uno o más tags del catálogo.
    - El formulario de creación permite elegirlos.
    - La lista y el detalle de cada discusión los muestran.
    - La API valida que los tags existan y evita duplicados.
    - Hay una migración de base de datos y tests de la relación discusiones–tags.
  - **Buscador y filtros:**
    - Se puede buscar por título, descripción o nombre del juego.
    - Se puede filtrar por uno o varios tags; al combinar varios, solo aparecen las discusiones que tengan todos.
    - La búsqueda y los filtros se pueden combinar.
    - Se mantiene la paginación y se vuelve a la primera página al cambiar la búsqueda o los filtros.
    - Se muestran los estados de carga, error y sin resultados.
- **Mockup:** no hay mockup adjunto.
- **Tarjetas incluidas:** `tags de especificos de foro`, `Buscador y filtros para el listado de foros`.

### US-03: Guardar y revisar mis juegos favoritos

- **Actores:** usuario registrado.
- **Funcionalidad:** guardar cualquier juego como favorito, tener una sección "Mis favoritos" en la navbar y ver ahí la lista paginada. El icono de favorito aparece marcado también después de recargar la página.
- **Valor:** el usuario arma su propia biblioteca y vuelve rápido a los juegos que le gustaron.
- **Criterios de aceptación:**
  - Todos los juegos tienen la opción de guardar en favoritos.
  - Un juego guardado aparece en la página de favoritos del usuario.
  - La navbar tiene una sección "Mis favoritos" que lleva a esa página.
  - La página muestra los favoritos paginados de a 9, organizados de a 3.
  - El icono de favorito aparece marcado en la tarjeta del juego al recargar, si el juego está en la lista del usuario.
- **Mockup:** no hay mockup adjunto.
- **Tarjetas incluidas:** `guardar un juego en favoritos`, `crear pagina de juegos favoritos`, `Persistencia del icono favorito`.

### US-04: Reconocer a las personas de la comunidad (perfil y autoría)

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:**
  - La navbar muestra la imagen del perfil con sesión iniciada.
  - La página de cada juego muestra el nombre de quien lo subió, debajo de la descripción.
  - El perfil lista los foros que la persona creó, y al hacer clic en uno se llega a ese foro. Para ello, la lista pasó a mostrar la tarjeta del foro.
- **Valor:** da identidad y crédito a los autores y facilita recorrer la actividad de cada usuario.
- **Criterios de aceptación** (derivados de las descripciones):
  - La navbar muestra un pequeño icono con la imagen del perfil logueado.
  - En la página del juego aparece el nombre del uploader debajo de la descripción.
  - La página de perfil muestra los foros creados por la persona.
  - Al hacer clic en un foro del perfil, se abre la página de esa discusión.
- **Mockup:** no hay mockup adjunto.
- **Tarjetas incluidas:** `Poner imagen de perfil al navbar`, `Añadir creador al juego`, `Agregar los foros publicados a Profile`, `Al cliquear en un foro en el perfil lleva al foro`.

### US-05: Publicar juegos con control y sin exponer borradores

- **Actores:** autor de juegos, visitante y equipo de desarrollo.
- **Funcionalidad:**
  - Un flujo de publicación definido para los juegos.
  - Los juegos en borrador no son accesibles públicamente (ni en la búsqueda ni en sus archivos y portadas).
  - El seeder de datos de prueba ya no modifica juegos existentes.
- **Valor:** protege el trabajo de los autores hasta que decidan publicarlo, garantiza que el público solo vea contenido publicado y evita que los datos de prueba corrompan datos reales.
- **Criterios de aceptación:**
  - **Flujo de publicación:**
    - Queda definido quién puede publicar un juego.
    - Si hay revisión manual, el autor no puede publicar directamente su borrador.
    - Los estados y las acciones disponibles coinciden con el proceso acordado.
    - La interfaz explica claramente el estado del juego.
  - **Borradores:**
    - La búsqueda pública devuelve únicamente juegos `PUBLISHED`.
    - Las rutas de archivos y portadas no entregan recursos de juegos `DRAFT`.
    - Los juegos publicados siguen apareciendo y ejecutándose con normalidad.
    - Hay cobertura de tests para el acceso a juegos publicados y en borrador.
  - **Seeder:**
    - No cambia propietarios ni estados de juegos existentes.
    - Los datos de prueba se crean de forma segura y repetible.
    - La documentación aclara qué base de datos y perfil se pueden usar.
    - Una prueba verifica que los datos existentes no se modifican.
- **Mockup:** no hay mockup adjunto (la parte visual es solo el estado del juego en la interfaz).
- **Tarjetas incluidas:** `Definir y aplicar el flujo de publicación de juegos`, `Impedir el acceso público a juegos en borrador`, `Evitar que el seeder modifique juegos existentes`.

### US-06: Una interfaz más clara y consistente

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:** el nombre Ludarium con un icono visible en la página principal, toda la interfaz en un solo idioma (inglés), filtros de juegos y foros que se despliegan y ocultan, y mejoras visuales generales.
- **Valor:** la plataforma se percibe coherente y profesional, y los filtros no ocupan espacio cuando no se usan.
- **Criterios de aceptación** (derivados de las descripciones):
  - En algún lugar de la página principal figura el nombre Ludarium con un icono.
  - Todo el texto de la interfaz está en inglés, sin mezcla con español.
  - La UI de filtros de juegos y foros se puede ocultar y desplegar.
  - Se aplican mejoras visuales generales (la tarjeta no detalla cuáles).
- **Mockup:** no hay mockup adjunto.
- **Tarjetas incluidas:** `Agregar nombre a la pagina principal`, `Estandarizar idioma a ingles`, `Desplegable de filtros`, `visuales`.
