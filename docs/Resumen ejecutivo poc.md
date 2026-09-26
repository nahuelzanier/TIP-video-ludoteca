# Ludarium

## User Stories Implementadas — Sprint 1

**Proyecto final de carrera (UNQ)**  
**Integrantes:** Nahuel Zanier y Lucas Sanguinetti

Este documento describe, en términos de *user stories*, la funcionalidad efectivamente entregada durante el Sprint 1 del tablero de Trello del proyecto. Las tarjetas del sprint que representan tareas de front-end, back-end o infraestructura de una misma funcionalidad se agruparon dentro de una única historia de usuario, siguiendo el criterio de representar aquello que tiene sentido para el usuario final y no la estructura interna del tablero.

---

# Historia de Usuario 1: Explorar el catálogo de juegos disponibles

**Cards de Trello (Sprint 1) que componen esta historia:**

- Poder seleccionar entre juegos
- Cargar juegos
- Imágenes de juegos
- Crear proyecto en React
- Crear proyecto Spring Boot
- Combinar proyectos de React y Spring
- Backend básico conectado
- Agregar estilo

## Actor/es

**Jugador:** cualquier persona que ingresa a Ludarium buscando juegos para probar.

Es el único actor involucrado en esta historia durante el Sprint 1. Todavía no existe un rol diferenciado de desarrollador o uploader, ya que esa funcionalidad no se encuentra implementada en esta etapa del proyecto.

## En qué consiste la funcionalidad

Al ingresar a la aplicación, el usuario visualiza un catálogo de juegos donde cada elemento muestra:

- Miniatura o imagen representativa.
- Título.
- Breve descripción.

La información es obtenida desde un backend desarrollado en Spring Boot y consumida por un frontend desarrollado en React. Los juegos disponibles se encuentran previamente cargados en el servidor.

El objetivo es que el usuario pueda identificar rápidamente qué juegos están disponibles y seleccionar uno para jugar sin necesidad de instrucciones adicionales.

## Valor que aporta

Esta funcionalidad constituye la puerta de entrada al resto del sistema.

Sin un catálogo navegable, ninguna otra funcionalidad de la plataforma tendría sentido. Además de permitir descubrir contenido de forma simple y visual, establece la base técnica de comunicación entre frontend y backend sobre la que se apoyarán futuras funcionalidades.

## Criterios de aceptación

- La aplicación muestra una miniatura por cada juego disponible en formato lista o grilla.
- El usuario puede seleccionar cualquier juego mediante un único click.
- Existen al menos tres juegos distintos visibles en el catálogo.
- Los juegos y sus assets se almacenan en la carpeta de datos del proyecto (`gameData`) y permanecen persistidos dentro del repositorio.
- El listado es obtenido mediante una consulta real desde el frontend hacia el backend y no mediante datos hardcodeados en el cliente.

## Mockups

No existen mockups adjuntos en las tarjetas de Trello correspondientes a esta historia.

---

# Historia de Usuario 2: Jugar un juego dentro de la aplicación

**Cards de Trello (Sprint 1) que componen esta historia:**

- Agregar videojuegos en HTML

## Actor/es

**Jugador:** usuario que ya seleccionó un juego dentro del catálogo y desea ejecutarlo.

## En qué consiste la funcionalidad

Una vez seleccionado un juego, este se ejecuta directamente dentro de Ludarium mediante un `iframe`, evitando redirecciones a sitios externos o instalaciones adicionales.

La interacción del usuario con el juego queda aislada del resto de la interfaz. Los eventos generados dentro del juego no afectan el funcionamiento de la aplicación y, del mismo modo, la navegación por la aplicación no interfiere con el juego en ejecución.

## Valor que aporta

Esta es la funcionalidad central del producto.

Transforma a Ludarium de un simple catálogo en una plataforma donde los usuarios pueden jugar directamente. La propuesta de valor principal es reducir la fricción al mínimo posible: elegir un juego y comenzar a jugar con un solo click, sin instalaciones ni configuraciones adicionales.

## Criterios de aceptación

- El juego seleccionado se ejecuta dentro de la aplicación mediante un `iframe`.
- Las interacciones realizadas dentro del juego no disparan acciones en la interfaz de Ludarium.
- Las acciones realizadas fuera del `iframe` no afectan el estado del juego en ejecución.
- El juego se visualiza correctamente dentro del espacio embebido sin recortes ni superposiciones con otros elementos de la interfaz.
- Como alternativa válida, el juego puede ejecutarse en pantalla completa si eso resuelve mejor los problemas de integración visual.

## Mockups

No existen mockups adjuntos en la tarjeta de Trello correspondiente a esta historia.

---

# Tareas del Sprint 1 sin Historia de Usuario Asociada

Las siguientes tarjetas no fueron transformadas en historias de usuario debido a que no representan funcionalidades directamente utilizables por un actor del sistema, sino tareas de infraestructura, investigación o gestión del proyecto.

No obstante, se incluyen por transparencia y porque muchas de ellas constituyen prerrequisitos técnicos para las funcionalidades implementadas.

- **Crear repositorio:** creación del repositorio público en GitHub e incorporación del segundo integrante como colaborador.
- **Configurar CI:** integración continua para impedir compilaciones exitosas cuando los tests fallan.
- **Crear proyecto Spring Boot:** puesta en marcha del backend y configuración inicial de PostgreSQL.
- **Crear proyecto en React:** puesta en marcha del frontend y conexión inicial con la API.
- **Combinar proyectos de React y Spring:** integración entre frontend y backend.
- **Backend básico conectado:** validación de comunicación correcta entre ambas capas.
- **Elegir licencia:** definición de la licencia MIT para el proyecto.
- **Crear documentación vía GitHub:** configuración inicial del wiki del repositorio.
- **Añadir profesores a Trello:** incorporación de docentes al tablero de seguimiento.
- **Definir usuario de la aplicación:** análisis y definición del público objetivo.
- **Crear un nombre y logo:** definición de la identidad visual y de marca de Ludarium.
- **Agregar estilo:** aplicación de estilos visuales generales a la interfaz.
- **Investigar Flash:** investigación exploratoria sobre la posibilidad de ejecutar juegos Flash mediante Ruffle. No constituye funcionalidad entregada.

---

# Resumen Ejecutivo

Durante el Sprint 1 se establecieron las bases funcionales y técnicas de Ludarium.

Se implementó un frontend visual básico conectado a un backend que almacena y expone la información de los juegos disponibles. También se incorporaron los primeros videojuegos jugables dentro de la plataforma.

Como parte del diseño de la aplicación, se desarrolló un sistema de tarjetas (*cards*) que permite representar todos los juegos de forma uniforme y polimórfica, independientemente de sus características internas. Asimismo, se definió que la ejecución de los juegos se realizará mediante `iframes`, facilitando su integración dentro de la aplicación y reduciendo la complejidad técnica necesaria para ponerlos a disposición de los usuarios.

En cuanto a las decisiones arquitectónicas, se realizó una investigación preliminar sobre la incorporación de juegos desarrollados en Flash utilizando el emulador Ruffle. Sin embargo, se decidió posponer dicha funcionalidad por dos motivos principales:

1. La integración implica una complejidad técnica considerable para el valor que aporta al producto.
2. Flash es una tecnología obsoleta y actualmente no se desarrollan nuevos juegos utilizando ese formato.

Dado que el esfuerzo requerido no resulta proporcional al beneficio esperado, se optó por orientar la plataforma principalmente hacia juegos desarrollados con Godot, tecnología que ofrece mejores perspectivas de crecimiento y mantenimiento.

No obstante, la arquitectura implementada mantiene la posibilidad de incorporar en el futuro otros tipos de juegos que puedan ejecutarse dentro de `iframes`, siempre que aporten valor a la plataforma.
