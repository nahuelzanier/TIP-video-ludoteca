# Ludarium: resumen ejecutivo de lo que está en Testing

**Tablero:** Proyecto videojuegos (Trello)
**Fecha del resumen:** 24/10/2026
**Alcance:** las 12 tarjetas de la columna *Testing*, agrupadas en 5 user stories y 1 historia técnica.

**Actores:**

- **Visitante:** persona sin sesión iniciada.
- **Usuario registrado:** persona con sesión iniciada.
- **Autor de juegos:** usuario registrado que sube juegos HTML.
- **Equipo de desarrollo:** responsables de calidad y seguridad de la plataforma.

---

## Resumen ejecutivo

### Qué se agregó o modificó en esta iteración

Esta iteración arranca después del cierre de las funcionalidades base del Sprint 2 (navbar, inicio de sesión, búsqueda, perfil, subir juego y comentarios/valoraciones, todas cerradas el 14/10). Sobre esa base se pasaron a Testing entre el 14 y el 24 de octubre:

- **Descubrimiento de juegos:** filtros combinables, selección aleatoria y recomendaciones de juegos similares en cada página de juego.
- **Navegación y acceso:** navegación completa entre secciones, y redirección al inicio de sesión cuando una acción lo requiere.
- **Foro:** el listado de discusiones de la plataforma y la creación de nuevas discusiones.
- **Personalización del perfil:** foto de perfil, fondo de página y tema claro u oscuro.
- **Seguridad:** aislamiento de los juegos subidos mediante `sandbox` en el iframe y revisión de la protección de operaciones autenticadas (CSRF).
- **Calidad:** cobertura de tests del back end superior al 80%, exigida como condición del build.

**Esfuerzo estimado en Testing:** 34 puntos, según las etiquetas numéricas de las tarjetas.

### Decisiones y desafíos

1. **La seguridad pasó a ser prioridad alta en medio del sprint.**
   - Se detectó que los juegos subidos (HTML y JavaScript) se servían desde el mismo origen que la API y que el iframe no tenía `sandbox`. Un juego malicioso podría haber usado la sesión del usuario para hacer operaciones autenticadas.
   - Se creó una tarjeta de 13 puntos ("Aislar los juegos subidos del origen de Ludarium") y se la dividió en partes:
     - Separar el origen en desarrollo local: **hecho**.
     - `sandbox` del iframe y revisión de CSRF: **en Testing**.
     - Verificar la compatibilidad de los juegos con el aislamiento y documentar la configuración: **en Backlog**.
2. **El aislamiento en producción se pospuso a propósito.** Depende de definir el proveedor de hosting y los dominios. Quedó en Backlog (8 puntos) con requisitos claros: dominio de juegos separado, cookies `HttpOnly`/`Secure` sin atributo `Domain`, y CORS limitado al frontend.
3. **El permiso del iframe es lo mínimo necesario.** Se permite ejecutar los scripts del juego, pero se lo aísla del DOM y del almacenamiento de Ludarium. Los permisos adicionales solo se habilitarán si algún juego los necesita.
4. **El alcance del foro se recortó.** La idea original incluía navegar, responder y valorar discusiones. En esta iteración quedaron solo el listado y la creación de discusiones. Responder, valorar y las páginas de detalle pasaron al Backlog.
5. **Salió a la luz deuda técnica y de producto (Backlog, prioridad alta):**
   - La búsqueda pública no filtra por estado, y los archivos de juegos en borrador son accesibles.
   - El seeder del perfil `dev` pisa propietarios y estados de juegos existentes.
   - Falta definir si la publicación de juegos es inmediata o pasa por revisión manual.
6. **La calidad pasa a ser una condición del build.** La aplicación no debe levantar si la cobertura no alcanza el umbral.

---

## User stories

### US-01: Navegar por toda la aplicación

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:** moverse entre todas las partes de la plataforma (entrar y salir de juegos, de perfiles, del foro, etc.) y volver siempre a la página principal desde la navbar.
- **Valor:** el usuario no se pierde y puede explorar la plataforma de forma fluida.
- **Criterios de aceptación:**
  - Todas las páginas tienen su propio endpoint, descrito con buenas prácticas.
  - Desde la navbar siempre se puede volver a la página principal.
  - Se puede entrar y salir de juegos y de perfiles sin quedar en un callejón sin salida.
- **Mockup:** no hay mockup adjunto en la tarjeta.
- **Tarjetas incluidas:** `navegacion`.

### US-02: Descubrir juegos (filtrar, azar y recomendaciones)

- **Actores:** visitante y usuario registrado.
- **Funcionalidad:**
  - Filtrar los resultados de una búsqueda (tipo de juego, cantidad de jugadores, género, valoraciones, tags).
  - Abrir un juego al azar desde la navbar.
  - Ver, al final de cada página de juego, juegos similares.
- **Valor:** reduce el tiempo que el usuario tarda en encontrar algo que quiera jugar y fomenta la exploración.
- **Criterios de aceptación:**
  - **Filtros:**
    - Una vez hecha una búsqueda existe la opción de filtrar los resultados.
    - Se pueden agregar tags y criterios como la valoración del juego.
    - Los filtros se pueden combinar entre sí.
  - **Aleatorio:**
    - La navbar tiene un botón o sección "aleatorio".
    - Ese botón abre la página de un juego al azar entre todos los de la aplicación.
  - **Recomendaciones:**
    - Al final de la página de un juego aparece una sección "recomendaciones" con 3 juegos que comparten tags (al menos dos).
    - Cada recomendado se muestra solo con imagen y título.
    - Al hacer clic se llega a la página de ese juego.
- **Mockup:** no hay mockup adjunto en las tarjetas.
- **Tarjetas incluidas:** `filtros`, `seleccion aleatoria`, `recomendaciones`.

### US-03: Iniciar sesión cuando una acción lo requiere

- **Actores:** visitante.
- **Funcionalidad:** el visitante es guiado al inicio de sesión cuando quiere hacer algo que exige cuenta.
  - En lugar del nombre de perfil, ve un botón "Iniciar sesión".
  - Si intenta comentar sin sesión, es redirigido al inicio de sesión.
- **Valor:** hace evidente cómo participar en la comunidad y evita acciones fallidas sin explicación.
- **Criterios de aceptación:**
  - Sin sesión, la navbar muestra el botón "Iniciar sesión" donde iría el nombre de perfil, y lleva a la página de inicio de sesión.
  - Al presionar "subir comentario" sin sesión, el usuario es redirigido a la página de inicio de sesión.
- **Mockup:** no hay mockup adjunto en las tarjetas.
- **Tarjetas incluidas:** `redireccionamiento a la pagina de inicio de sesion`, `los comentarios requieren sesion`.

### US-04: Participar en el foro de la comunidad

- **Actores:** visitante (consulta el foro) y usuario registrado (crea discusiones).
- **Funcionalidad:** ver el listado de discusiones de la plataforma y crear una nueva con título, descripción y tags opcionales.
- **Valor:** crea un espacio de conversación y opinión sobre los juegos, lo que aporta comunidad y retención.
- **Criterios de aceptación:**
  - **Listado:**
    - Existe una sección "foros" con todas las discusiones.
    - Cada discusión muestra nombre, autor, icono del autor, descripción breve y tags.
    - Al hacer clic se abre la página de esa discusión.
  - **Creación:**
    - Hay un botón en la sección foro para subir una discusión.
    - Título y descripción son obligatorios; si faltan, aparece una alerta de requisitos mínimos.
    - Los tags son opcionales.
    - Al subirla, el usuario es llevado a la página de esa discusión.
    - La nueva discusión queda en el tope del listado.
- **Mockup:** no hay mockup adjunto en las tarjetas.
- **Tarjetas incluidas:** `foro`, `agregar una discusion al foro`.

### US-05: Personalizar mi perfil y la apariencia

- **Actores:** usuario registrado.
- **Funcionalidad:** cambiar la foto de perfil, subir un fondo de página tipo wallpaper y elegir tema claro u oscuro.
- **Valor:** el usuario siente la plataforma como propia y la adapta a su preferencia visual.
- **Criterios de aceptación:**
  - Al hacer clic en su propia imagen, el usuario puede cargar otra como foto de perfil.
  - Puede subir un fondo tipo wallpaper que se muestra detrás del perfil y cubre el resto de la pantalla.
  - Desde la configuración puede elegir tono oscuro o claro.
- **Mockup:** no hay mockup adjunto en la tarjeta.
- **Tarjetas incluidas:** `visuales`.

### US-06: Jugar con seguridad (juegos subidos aislados de mi sesión)

- **Actores:** usuario registrado y autor de juegos; el equipo de desarrollo es responsable de la solución.
- **Funcionalidad:** los juegos subidos por terceros (HTML y JavaScript) se ejecutan en un iframe con `sandbox` que permite ejecutar scripts del juego pero lo aísla del DOM y del almacenamiento de Ludarium. Además se verifica que las operaciones que modifican datos sigan protegidas por CSRF y que un juego aislado no pueda usarlas con la sesión del usuario.
- **Valor:** el usuario puede jugar sin riesgo de que un juego malicioso robe su sesión o haga operaciones en su nombre.
- **Criterios de aceptación:**
  - Un juego no puede acceder al DOM ni al almacenamiento de Ludarium.
  - Un juego no puede usar la sesión de Ludarium para hacer operaciones autenticadas.
  - Las operaciones que modifican datos siguen protegidas por CSRF.
  - Los juegos existentes siguen ejecutándose correctamente.
- **Mockup:** no aplica, no tiene interfaz visible.
- **Tarjetas incluidas:** `SEGURIDAD - Agregar sandbox al iframe de los juegos`, `SEGURIDAD - Revisar la proteccion de operaciones autenticadas`.
- **Contexto:** continúa el trabajo ya cerrado de separar el origen de los juegos en desarrollo local. Queda pendiente en Backlog el aislamiento de origen en producción.

### Historia técnica: cobertura de tests del back end

- **Actores:** equipo de desarrollo.
- **Funcionalidad:** añadir tests automáticos del back end con cobertura mínima exigida.
- **Valor:** da confianza para seguir sumando funcionalidades sin romper lo existente.
- **Criterios de aceptación:**
  - La cobertura del back end supera el 80%.
  - Los tests cubren la mayor cantidad posible de casos.
  - La aplicación no levanta si la cobertura es insuficiente.
  - Todos los tests pasan.
- **Tarjetas incluidas:** `añadir test de coverage`.

---

## Puntos a revisar antes del cierre

- **Foro:** el Backlog todavía tiene tarjetas de "Crear la página de listado de foros", "Crear las páginas de detalle y creación de discusiones" e "Implementar valoraciones". La primera dice que `/forum` muestra una página de prueba, lo que contradice el estado de `foro` en Testing. Conviene confirmar qué está realmente implementado para evitar duplicados. La tarjeta `foro` además pasó a *Sprint 2 Done* y volvió a Testing el mismo día (16/10).
- **Selección aleatoria:** la descripción menciona prefiltrar por tags, pero los criterios de aceptación solo piden el botón. Si el prefiltrado debe validarse, falta incluirlo en los criterios.
- **Seguridad:** las dos tarjetas de Testing no tienen criterios de aceptación propios. Los criterios de la US-06 se tomaron de la tarjeta madre ya cerrada ("Aislar los juegos subidos del origen de Ludarium") y deberían copiarse a las tarjetas.
- **Mockups:** ninguna tarjeta del tablero tiene adjuntos. Si hay capturas o bocetos, conviene agregarlos a las US-01 a US-05.
