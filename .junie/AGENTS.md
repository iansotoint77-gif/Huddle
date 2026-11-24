# PROMPT MAESTRO PARA JUNIE — Huddle (tipo Spond, baloncesto)

> Pégalo entero en Junie UNA sola vez, en un proyecto vacío. Junie guardará el contexto
> en `.junie/AGENTS.md` y trabajará por FASES, parándose al final de cada una.
> Para continuar, escribe solo: `Continúa con la FASE N`.

---

## 0. ROL Y FORMA DE TRABAJAR

Eres un desarrollador Java senior que me ayuda en una entrega de "Acceso a Datos" (2.º DAM).
Yo soy el arquitecto y revisor: tú implementas, yo reviso y corrijo.

Reglas de trabajo:
1. Lo primero que haces: crear `.junie/AGENTS.md` con TODO este documento (secciones 1 a 11) para no perder contexto entre fases.
2. Trabaja por fases (sección 11). Al terminar una fase: resume qué has hecho, ejecuta la comprobación de esa fase, y PARA. No empieces la siguiente sin que yo te lo pida.
3. No hagas `git commit`. Al final de cada fase dame la lista de commits atómicos sugeridos (formato Conventional Commits, en español, p. ej. `feat(dao): añade EventoDaoJdbc con PreparedStatement`) y yo los ejecuto.
4. Al final de cada fase añade una entrada a `AI_WORKFLOW.md` con: prompt de la fase, decisiones de arquitectura y por qué. NO inventes errores: la sección "Errores corregidos manualmente" la relleno yo.
5. Si una decisión no está especificada, elige la opción más simple que cumpla los requisitos y anótala en `AI_WORKFLOW.md`.
6. Cuando algo no compile o falle, corrígelo antes de terminar la fase. No me entregues código sin compilar.

---

## 1. LA APP

"Huddle": app web tipo Spond para equipos de baloncesto.
- Un usuario crea un EQUIPO y pasa a ser su ENTRENADOR (administrador).
- El entrenador añade jugadores al equipo (por email, con dorsal) y crea EVENTOS: ENTRENAMIENTO o PARTIDO.
- Cada jugador VOTA su asistencia a cada evento: VOY / NO_VOY / QUIZAS (con comentario opcional).
- El entrenador ve el resumen de asistencia por evento (quién va, quién no, quién no ha respondido).

**Sin login real (decisión consciente, documentarla en el README como limitación conocida):** el frontend tiene un selector de "usuario actual", guarda su id y lo envía en la cabecera `X-Usuario-Id` en cada petición. Un `Filter` la lee y la deja disponible para el servicio. La capa de servicio SIEMPRE recibe `usuarioId` como parámetro (así cambiar a login real solo cambiaría de dónde sale ese id).

---

## 2. STACK (cerrado, no cambies nada)

- Java 17, Maven, packaging `war`. Sin Spring ni ORM: **JDBC puro** con `PreparedStatement`, `try-with-resources` y pool **HikariCP**.
- Servlets (Jakarta, **Tomcat 10.1**) que devuelven JSON con **Gson**.
- MySQL 8 (`mysql-connector-j`).
- Tests: **JUnit 5**, **Mockito** y **Testcontainers (MySQL)** para los tests de integración del DAO.
- Frontend: HTML5 semántico, CSS3 y JavaScript moderno (`fetch`), sin frameworks ni librerías de UI. Nada de Java ni SQL en las vistas.
- Paquete base: `es.huddle` (artifactId `huddle`).

---

## 3. ARQUITECTURA POR CAPAS (desacoplamiento estricto)

Flujo único: **Controller → Service → DAO → BD**. Ninguna capa salta a otra.

```
es.huddle
├── config        DataSourceProvider (HikariCP, SOLO System.getenv), AppConfig
├── model         Entidades (Usuario, Equipo, MiembroEquipo, Evento, Respuesta) + enums (Rol, TipoEvento, EstadoAsistencia)
├── dto           DTOs de entrada/salida (nunca se serializan entidades) + mappers
├── dao           Interfaces + implementaciones JDBC (UsuarioDao, EquipoDao, MiembroDao, EventoDao, RespuestaDao)
├── service       Reglas de negocio y transacciones (EquipoService, EventoService, RespuestaService, UsuarioService)
├── controller    Servlets (JSON) + BaseServlet con manejo central de excepciones
├── filter        UsuarioFilter (lee X-Usuario-Id), CorsFilter si hace falta, EncodingFilter UTF-8
├── exception     NotFoundException, DuplicateException, ForbiddenException, ValidationException, DataAccessException
└── util          JsonUtil (Gson, formato de fecha ISO-8601), TransactionManager
```

Reglas de arquitectura:
- **DTOs obligatorios** entre capas: los controllers y el JSON solo conocen DTOs.
- **DAO**: solo `PreparedStatement` (PROHIBIDO concatenar SQL con `Statement`), `try-with-resources` siempre, las `SQLException` se envuelven en `DataAccessException` (nunca se filtran hacia arriba).
- **Transacciones** en la capa Servicio mediante `TransactionManager` (commit/rollback, conexión por hilo). Obligatorio en operaciones de varios pasos, p. ej. crear equipo = insertar equipo + insertar al creador como ENTRENADOR (todo o nada).
- **Validación en servidor** en el Servicio (campos obligatorios, longitudes, formatos, reglas de negocio). Aunque el frontend valide, el servidor es la autoridad.
- **Manejo de errores centralizado** en `BaseServlet`: traduce excepciones a JSON `{"status":409,"error":"Conflict","message":"..."}`.
- **Códigos HTTP semánticos**: 200 OK, 201 Created (con `Location`), 204 No Content (borrados), 400 Bad Request (validación / JSON mal formado), 403 Forbidden (permisos), 404 Not Found, 409 Conflict (duplicados), 500 Internal Server Error (sin filtrar detalles internos al cliente; el detalle va al log).

---

## 4. BASE DE DATOS (MySQL 8) — `db/init.sql`

Charset `utf8mb4`, motor InnoDB, claves foráneas con `ON DELETE` / `ON UPDATE` explícitos. Script con DDL + datos de prueba, montado en `/docker-entrypoint-initdb.d/`.

| Tabla | Columnas | Restricciones |
|---|---|---|
| `usuarios` | id PK AI, nombre, email | `UNIQUE(email)` |
| `equipos` | id PK AI, nombre, categoria, creador_id | FK creador_id → usuarios `ON DELETE RESTRICT ON UPDATE CASCADE` |
| `miembros_equipo` | equipo_id, usuario_id, rol ENUM('ENTRENADOR','JUGADOR'), dorsal NULL | PK(equipo_id, usuario_id); `UNIQUE(equipo_id, dorsal)`; FKs `ON DELETE CASCADE ON UPDATE CASCADE` — **N:M usuarios↔equipos** |
| `eventos` | id PK AI, equipo_id, tipo ENUM('ENTRENAMIENTO','PARTIDO'), titulo, fecha_hora DATETIME, lugar, rival NULL | FK equipo_id → equipos `ON DELETE CASCADE ON UPDATE CASCADE` — **1:N equipos→eventos** |
| `respuestas` | evento_id, usuario_id, estado ENUM('VOY','NO_VOY','QUIZAS'), comentario NULL, fecha_respuesta | PK(evento_id, usuario_id); FKs `ON DELETE CASCADE ON UPDATE CASCADE` — **N:M eventos↔usuarios** |

Datos de prueba (usa fechas relativas con `DATE_ADD(NOW(), INTERVAL n DAY)` para que siempre haya eventos futuros votables):
- Usuarios: Carlos Ruiz (entrenador), Lucas Pérez, Marcos Díaz, Hugo Sanz, Daniel Vega, Álex Moreno, Pablo Gil, Sofía Gil.
- Equipos: "Minibasket 2016" (creador Carlos, 6 jugadores con dorsal), "Minibasket 2018" (Carlos) y "Cadete Femenino" (Sofía entrenadora, Carlos como jugador, para demostrar roles distintos por equipo).
- Eventos: 3 próximos en Minibasket 2016 (2 entrenamientos + 1 partido vs CB Pozuelo), 2 en los otros equipos, 2 pasados.
- Respuestas de ejemplo con los 3 estados y algún jugador SIN responder.

---

## 5. CONFIGURACIÓN EXTERNALIZADA

PROHIBIDO hardcodear credenciales o URLs de conexión en Java. `DataSourceProvider` lee SOLO variables de entorno: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (y `DB_POOL_SIZE` opcional) y falla con un mensaje claro si falta alguna.
Se definen en `.env` (en `.gitignore`) y se inyectan por Docker Compose. Entrega `.env.example` con valores de ejemplo y comentarios.

---

## 6. DOCKER COMPOSE

- `Dockerfile` **multi-stage**: stage 1 `maven:3.9-eclipse-temurin-17` compila el `.war` con `mvn -DskipTests package` (usa caché de dependencias: copia primero el `pom.xml`); stage 2 `tomcat:10.1-jdk17` despliega el war como `ROOT.war`. Usuario no root si es viable.
- `docker-compose.yml` con 3 servicios:
  - `db`: `mysql:8`, variables desde `.env`, `init.sql` montado en `/docker-entrypoint-initdb.d/`, **volumen nombrado** `db_data` (persistencia demostrable), **healthcheck** (`mysqladmin ping` con credenciales), puerto del host configurable, `TZ=Europe/Madrid`.
  - `app`: build local, `depends_on: db: condition: service_healthy`, variables de entorno desde `.env` (`DB_HOST=db`), puerto 8080, `restart: unless-stopped`.
  - `adminer`: auditoría visual de la BD (puerto 8081), `depends_on` db.
- **Red interna** propia (`huddle_net`). La BD no se publica al host salvo que `.env` lo pida.
- Debe funcionar con: `docker compose up --build -d`. Tras `docker compose down` (sin `-v`) los datos deben persistir.

---

## 7. API REST (JSON, prefijo `/api`)

Cabecera `X-Usuario-Id` obligatoria excepto en `/api/usuarios`.

| Método y ruta | Acción | Éxito | Errores |
|---|---|---|---|
| `GET /api/usuarios` | Lista usuarios (para el selector) | 200 | — |
| `POST /api/usuarios` | Crea usuario (nombre, email) | 201 | 400, 409 (email repetido) |
| `GET /api/equipos` | Equipos del usuario actual + su rol en cada uno + nº jugadores + nº eventos próximos | 200 | 404 usuario |
| `POST /api/equipos` | Crea equipo; el creador queda como ENTRENADOR (transacción) | 201 | 400 |
| `GET /api/equipos/{id}` | Detalle del equipo + rol del usuario actual | 200 | 403 (no miembro), 404 |
| `GET /api/equipos/{id}/miembros` | Plantilla | 200 | 403, 404 |
| `POST /api/equipos/{id}/miembros` | Añade jugador por email + dorsal (solo ENTRENADOR) | 201 | 400, 403, 404 (usuario/equipo), 409 (dorsal u usuario repetido) |
| `PATCH /api/equipos/{id}/miembros/{usuarioId}` | Cambia dorsal (solo ENTRENADOR) | 200 | 400, 403, 404, 409 |
| `DELETE /api/equipos/{id}/miembros/{usuarioId}` | Quita jugador (solo ENTRENADOR; no puede quitarse a sí mismo) | 204 | 400, 403, 404 |
| `GET /api/equipos/{id}/eventos?tiempo=proximos\|pasados` | Eventos con resumen de votos (voy/no voy/quizás/sin responder) | 200 | 403, 404 |
| `POST /api/equipos/{id}/eventos` | Crea evento (solo ENTRENADOR) | 201 | 400, 403, 404 |
| `GET /api/eventos/{id}` | Detalle del evento | 200 | 403, 404 |
| `PUT /api/eventos/{id}` | Edita evento (solo ENTRENADOR) | 200 | 400, 403, 404 |
| `DELETE /api/eventos/{id}` | Borra evento (solo ENTRENADOR; borra respuestas en cascada) | 204 | 403, 404 |
| `GET /api/eventos/{id}/respuestas` | Resumen + lista de jugadores con su estado (incluye "SIN_RESPONDER") | 200 | 403, 404 |
| `PUT /api/eventos/{id}/respuesta` | El usuario actual vota (upsert: votar otra vez actualiza) | 200 | 400 (estado inválido / evento pasado), 403 (no es miembro), 404 |

### Reglas de negocio (capa Servicio)
1. Solo el ENTRENADOR del equipo crea/edita/borra eventos y gestiona miembros → si no, `ForbiddenException` (403).
2. Un jugador solo vota por sí mismo y solo en eventos de equipos a los que pertenece (403 si no).
3. Un voto por jugador y evento: votar de nuevo actualiza (upsert).
4. No se puede votar en eventos ya pasados (400).
5. El dorsal no se repite dentro del equipo (409) y debe estar entre 0 y 99.
6. Un PARTIDO exige rival; un ENTRENAMIENTO no lo lleva (se ignora/limpia).
7. Al crear un evento la fecha debe ser futura (400).
8. Nombre de equipo, categoría, título, lugar y email son obligatorios (400); email con formato válido.
9. Equipo o evento inexistente → 404. Se comprueba la existencia ANTES de los permisos.
10. El creador del equipo no puede ser eliminado del equipo.

---

## 8. FRONTEND — respetar los wireframes

Los wireframes están en `docs/wireframes/` (HTML + PNG, 10 vistas). **Ábrelos y reprodúcelos fielmente** (estructura, textos, jerarquía, componentes). Las cajas amarillas "Nota: …" son anotaciones técnicas del wireframe: **NO forman parte de la interfaz**.

Vistas → ficheros en `src/main/webapp`:
| Wireframe | Fichero |
|---|---|
| 01 Login simulado | `index.html` |
| 02 Mis equipos | `equipos.html` |
| 03 Crear equipo | `equipo-nuevo.html` |
| 04 Equipo · Eventos y 05 Equipo · Plantilla | `equipo.html?id=` (pestañas Eventos / Plantilla) |
| 06 Nuevo evento (y editar) | `evento-nuevo.html?equipo=` |
| 07 Asistencia (jugador) y 08 Asistencia (entrenador) | `evento.html?id=` (la vista cambia según el rol del usuario) |
| 09 Estados y errores | componentes transversales (ver abajo) |
| 10 Móvil | misma app, diseño responsive (breakpoint ≤ 640 px, mínimo 360 px) |

Estructura: `css/styles.css` (variables CSS con la paleta), `js/api.js` (wrapper de `fetch` que añade `X-Usuario-Id`, parsea el JSON de error y lanza un error tipado), `js/ui.js` (skeleton de carga, vacío, banners de error, toasts, confirmación de borrado), `js/<pagina>.js` por página. JS moderno (módulos ES, `async/await`), sin `innerHTML` con datos sin escapar (evita XSS: usa `textContent` o escapa).

### Paleta de color (variables en `:root`)
```
--navy:   #182440   barra superior, rol Entrenador, texto de títulos
--ink:    #1f2a44   texto principal
--label:  #3c4864   etiquetas de formulario
--orange: #e6600a   color de marca: botón principal, pestaña activa, logo, chip "Partido"
--orangeT:#ffecd9   fondo suave naranja (caja de fecha, escudo)
--green:  #2e7d32   VOY (chip sólido)      --greenT:#e2f2e3  --greenX:#1b5e20
--redT:   #fde2e2   NO VOY (fondo)          --redX:#b71c1c   NO VOY / errores / "Sin responder"
--amberT: #fff3cd   QUIZÁS (fondo)          --amberS:#f5a623 borde   --amberX:#854d0e
--bg:     #f0f4fa   fondo de la página      --blueT:#e9eff9 superficies suaves
--line:   #cdd6e4   bordes                  --mut:#6b7590    texto secundario
```
Logo: cuadrado redondeado naranja (`--orange`) con una «H» blanca en negrita. Tipografía: `Inter, system-ui, sans-serif`. Botón principal naranja con texto blanco; secundario blanco con borde; peligroso blanco con texto rojo. Tarjetas blancas, borde `--line`, radio 10 px.

### Estados obligatorios (vista 09) — rúbrica "Interfaz web"
- **Cargando**: skeleton/placeholder mientras llega cada `fetch`; botones deshabilitados con texto "Guardando…" durante los envíos.
- **Lista vacía**: mensaje + botón de acción (p. ej. "Aún no tienes equipos").
- **Errores claros según HTTP**: 400 (mensaje bajo el campo y campo resaltado), 403, 404, 409, 500 (con botón "Reintentar"), y error de red.
- **Éxito**: toast/banner verde ("Respuesta guardada correctamente").
- Sin errores en la consola del navegador. Navegación fluida (migas de pan, enlaces coherentes) y redirección a `index.html` si no hay usuario seleccionado.
- CRUD completo en la UI: equipos (crear, listar, ver), miembros (alta, cambio de dorsal, baja), eventos (crear, listar, editar, borrar), votos (votar/cambiar y ver resumen).

---

## 9. TESTS (JUnit 5)

- **Servicio** con Mockito (DAOs simulados): camino feliz de cada operación + casos límite: dorsal duplicado (409), email duplicado, equipo/evento inexistente (404), jugador que intenta crear evento (403), voto de un no miembro (403), voto en evento pasado (400), partido sin rival (400), voto repetido = actualización, rollback si falla la 2.ª inserción al crear equipo.
- **DAO** con **Testcontainers MySQL** cargando `init.sql`: CRUD completo de cada DAO + casos límite: clave duplicada (`UNIQUE`), registro inexistente (`Optional.empty()` / 0 filas), violación de FK, `ON DELETE CASCADE` (al borrar un evento se borran sus respuestas), upsert de respuestas.
- Nombres de test descriptivos (`crearEvento_siNoEsEntrenador_lanzaForbiddenException`).
- `mvn test` debe pasar en verde (los tests de DAO requieren Docker en marcha; el Dockerfile compila con `-DskipTests`).

---

## 10. DOCUMENTACIÓN Y ENTREGA

**README.md** (profesional):
1. Descripción de la app y captura(s) de las vistas.
2. **Diagrama Entidad-Relación** (Mermaid `erDiagram`).
3. **Esquema de arquitectura de contenedores** (Mermaid: navegador → app/Tomcat → db/MySQL, adminer, red `huddle_net`, volumen `db_data`).
4. Arquitectura por capas (paquetes y flujo Controller → Service → DAO → BD, DTOs).
5. **Variables de entorno** (tabla con descripción) y pasos: `cp .env.example .env` → editar → `docker compose up --build -d`.
6. **Credenciales de prueba** (usuarios del `init.sql`; app sin contraseña), **puertos expuestos** (8080 app, 8081 Adminer, BD) y credenciales de Adminer.
7. **Manual breve de uso** (flujo entrenador y flujo jugador, cómo cambiar de usuario).
8. Tabla de endpoints y códigos HTTP.
9. Cómo ejecutar los tests.
10. **Demostración de persistencia**: `docker compose down` + `up` conserva datos; `down -v` los borra.
11. Limitaciones conocidas (login simulado sin autenticación real) y decisiones de diseño.

**AI_WORKFLOW.md**: sección por fase (prompt, decisiones arquitectónicas, resultado) + sección vacía "Errores corregidos manualmente por el alumno" que rellenaré yo.

**Git**: `.gitignore` (`target/`, `.env`, `.idea/`, `*.iml`, `*.log`) y `.env.example`. Commits atómicos y descriptivos (los hago yo con tu lista).

**Entregable**: repo Git + `.zip` sin `target/` ni `.git/`.

---

## 11. FASES (una por vez; para al terminar cada una)

**FASE 1 — Esqueleto, BD y Docker.**
Crea `.junie/AGENTS.md`, estructura Maven y paquetes vacíos, `pom.xml` (Gson, HikariCP, mysql-connector-j, Servlet API jakarta `provided`, JUnit 5, Mockito, Testcontainers), `db/init.sql` completo (DDL + datos), `Dockerfile` multi-stage, `docker-compose.yml`, `.env.example`, `.gitignore`, `DataSourceProvider`, `README.md` y `AI_WORKFLOW.md` con títulos, y copia los wireframes a `docs/wireframes/` (te los dejo yo en esa carpeta). Un servlet `GET /api/health` que haga `SELECT 1`. **Comprobación**: `docker compose up --build -d` levanta db (healthy), app y adminer, y `http://localhost:8080/api/health` responde 200.

**FASE 2 — Modelo, DTOs, excepciones y DAO + tests DAO.**
Entidades, enums, DTOs, mappers, excepciones, `TransactionManager`, interfaces y DAOs JDBC, y los tests de integración del DAO (sección 9). **Comprobación**: `mvn test` en verde.

**FASE 3 — Capa de Servicio + tests.**
Servicios con todas las reglas de la sección 7 y transacciones, y sus tests con Mockito. **Comprobación**: `mvn test` en verde.

**FASE 4 — Controladores y API.**
Filters, `BaseServlet` con manejo central de errores, todos los endpoints de la sección 7 con los códigos HTTP indicados. **Comprobación**: prueba con `curl` los casos 200/201/204/400/403/404/409 y dime los resultados.

**FASE 5 — Frontend.**
Todas las vistas de la sección 8, fieles a los wireframes y a la paleta, con estados de carga/vacío/error/éxito y responsive. **Comprobación**: recorre el flujo entrenador y jugador sin errores en consola.

**FASE 6 — Documentación y revisión final.**
Completa `README.md` (sección 10) y `AI_WORKFLOW.md`. Haz una auditoría final contra esta checklist y dime qué falta: sin `Statement` concatenado · sin credenciales en el código · DTOs en toda la API · try-with-resources · healthcheck + `service_healthy` · volumen nombrado · red interna · `.env.example` · tests en verde con casos límite · README completo · sin errores de consola.

---

**Empieza ahora SOLO con la FASE 1 y para al terminar.**