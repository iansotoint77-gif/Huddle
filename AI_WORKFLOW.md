# Registro de trabajo asistido por IA

## Fase 1 — Esqueleto, base de datos y Docker

### Prompt

Implementar únicamente la Fase 1 del prompt maestro: estructura Maven, base de datos, Docker, configuración JDBC y endpoint de salud.

### Decisiones de arquitectura

- Java 17, `es.huddle:huddle` y empaquetado WAR para alinearse con Tomcat 10.1 y el stack especificado.
- Las credenciales y los parámetros de conexión se leen mediante variables de entorno; HikariCP configura `MysqlDataSource` directamente, sin codificar una URL JDBC.
- Los servicios comparten una red bridge propia; MySQL no publica puertos por defecto y un overlay Compose opcional permite exponerlo explícitamente.
- El servlet de salud ejecuta `SELECT 1` y responde con JSON sin exponer detalles internos de conexión.

### Resultado

Esqueleto y recursos de Fase 1 creados. `docker compose up --build -d` construyó el WAR Java 17 y dejó app, Adminer y MySQL en ejecución; MySQL quedó healthy y `GET /api/health` devolvió HTTP 200 con `{"status":"UP"}` tras ejecutar `SELECT 1`. La configuración usa una red bridge propia y no publica MySQL al host por defecto.

## Fase 2 — Modelo, DTOs, excepciones y DAO

### Prompt

Implementar únicamente la Fase 2: modelo, DTOs y mappers, excepciones, `TransactionManager`, interfaces e implementaciones JDBC y tests de integración DAO. Eliminar la plantilla `org.example.Main` y ejecutar `mvn test` sin iniciar la Fase 3.

### Decisiones de arquitectura

- Las entidades se representan con `record` inmutables de Java 17; las fechas de MySQL `DATETIME` usan `LocalDateTime`, sin convertirlas a zona horaria.
- Los DAO reciben un `TransactionManager` compartido: usa una conexión asociada al hilo durante transacciones y conexiones autocerradas fuera de ellas; sentencias y resultados se cierran con `try-with-resources`.
- Los errores MySQL de clave duplicada (código 1062) se traducen a `DuplicateException`; las demás `SQLException` quedan envueltas en `DataAccessException`.
- Las pruebas cargan `db/init.sql` directamente desde el classpath para reutilizar el esquema y datos canónicos. `docker-java.properties` fija `api.version=1.44` porque Docker Engine 29 exige API mínima 1.44 y Testcontainers 1.x usa una versión inferior por defecto; Testcontainers se actualiza a 1.21.3.

### Resultado

Se añadieron los cinco modelos, enums, DTOs y mappers, excepciones, interfaces y DAO JDBC completos, además de `TransactionManager` y tests de integración MySQL con Testcontainers. La primera ejecución de `mvn.cmd test` terminó correctamente con 8 tests. Se eliminó `org.example.Main`; no se inició la Fase 3.

### Revisión de cobertura de tests DAO

#### Prompt

Listar los ocho tests DAO existentes, compararlos con la sección 9, completar los casos faltantes y ejecutar `mvn test`, sin iniciar la Fase 3.

#### Decisiones de arquitectura

- Los escenarios de clave duplicada, violación de FK, cascada, upsert y CRUD de cada DAO ya estaban cubiertos; se mantuvieron sin duplicar tests.
- Se añadió una prueba transversal con IDs inexistentes para comprobar lecturas vacías y operaciones con cero filas afectadas en los cinco DAO, incluida la cobertura pendiente de `MiembroDao`.

#### Resultado

La suite quedó en 9 tests y `mvn test` finalizó correctamente: 9 ejecutados, 0 fallos, 0 errores y 0 omitidos.

## Fase 3 — Capa de Servicio y tests

### Prompt

Implementar únicamente la Fase 3 siguiendo `.junie/AGENTS.md`: servicios con las reglas de negocio y transacciones de la sección 7, tests Mockito y ejecutar `mvn test`, sin iniciar la Fase 4.

### Decisiones de arquitectura

- Los servicios reciben DTOs de entrada y devuelven DTOs; las entidades quedan dentro de la coordinación Servicio–DAO. Se añadieron DTOs específicos para detalle del equipo, resumen de equipos/eventos, plantilla con datos del usuario y resumen de asistencia.
- `ServiceValidation` centraliza campos obligatorios, longitudes, formato de email y rango de dorsales. Los servicios verifican existencia de equipo/evento antes de permisos, validan roles, y traducen duplicados DAO a mensajes de negocio.
- Crear equipo usa `TransactionManager` para insertar equipo y entrenador de forma atómica; ante fallo de la segunda inserción ejecuta rollback. Los resúmenes de asistencia se calculan sobre jugadores y marcan explícitamente `SIN_RESPONDER`.
- Los tests usan Mockito para los DAO; el test transaccional usa el `TransactionManager` real con un `DataSource` de seguimiento y una conexión proxy para verificar commit/rollback sin intentar instrumentar clases JDK con el Byte Buddy disponible en la JVM local.

### Resultado

Se implementaron `UsuarioService`, `EquipoService`, `EventoService` y `RespuestaService`, junto con validación reutilizable y DTOs de resumen. `mvn test` compiló con `--release 17` y terminó correctamente: 47 tests (9 DAO y 38 de servicio), 0 fallos, 0 errores y 0 omitidos. No se inició la Fase 4.

## Fase 4 — Controladores y API

### Prompt

Implementar la Fase 4: servlets JSON para los endpoints REST, filtro `X-Usuario-Id` con excepción de `/api/usuarios`, manejo centralizado de errores y filtros UTF-8. Levantar Compose, comprobar endpoints principales con curl y ejecutar `mvn test`, sin iniciar la Fase 5.

### Decisiones de arquitectura

- `AppConfig` crea una instancia compartida de DAO y servicios al iniciar el contexto web; los servlets reciben esos servicios desde el `ServletContext`, manteniendo el flujo Controller → Service → DAO.
- `BaseServlet` estandariza errores en JSON y los códigos 400/403/404/409; los errores inesperados se registran en el log y devuelven un mensaje genérico 500. Las creaciones devuelven 201 con `Location` y los borrados 204 sin cuerpo.
- `UsuarioFilter` publica un id positivo como atributo de petición y omite la cabecera en `/api/usuarios` y `/api/health`. `JsonUtil` centraliza Gson y serialización ISO-8601 de `LocalDateTime`.
- Se utiliza el mapeo de servlets por anotaciones y parámetros JSON DTO; no se añade CORS porque las vistas web se servirán desde el mismo origen.

### Resultado

`mvn test` compiló con Java 17 y terminó correctamente: 49 tests, 0 fallos, 0 errores y 0 omitidos. `docker compose up --build -d` reconstruyó la aplicación y dejó MySQL healthy, Tomcat y Adminer iniciados. Las pruebas HTTP devolvieron 200 en salud, listados y detalles; 201 en creación de evento y miembro con `Location`; 204 al borrar evento y miembro; 400 por cabecera ausente y validación de partido; 403 al crear como jugador; 404 para evento inexistente; y 409 por email/miembro duplicado. Se comprobó también que los votos se actualizan con PUT y se validó/limpió los recursos de prueba.

## Fase 5 — Frontend

### Prompt

Implementar únicamente el frontend HTML/CSS/JavaScript nativo de la Fase 5, conectado a la API, fiel a los diez wireframes, paleta y requisitos responsive/estados; levantar Compose y entregar la URL de revisión, sin iniciar la Fase 6.

### Decisiones de arquitectura

- Se crearon páginas HTML semánticas y módulos ES por vista; `api.js` centraliza `fetch`, `X-Usuario-Id`, errores tipados y rutas, mientras `ui.js` concentra carga, vacío, errores, selección de usuario, mensajes y confirmaciones.
- Los datos de API se insertan con `textContent`/nodos DOM, nunca con `innerHTML`; el usuario actual queda en `localStorage` y se envía en cada petición autenticada.
- La creación de equipo usa un selector de categorías y los eventos controles segmentados, siguiendo los controles dibujados en los wireframes. La asistencia adapta su jerarquía entre escritorio y móvil; el filtro de estados del entrenador se implementa en cliente sobre el resumen recibido.
- El menú móvil compacta la cabecera y permite desplegar navegación y selector de usuario; las notas amarillas de los wireframes se trataron como anotaciones, no como elementos de interfaz.

### Resultado

Se implementaron las seis vistas (selector de usuario, equipos, equipo/eventos, plantilla, formularios de alta/edición y asistencia por rol), con operaciones CRUD y estados de carga, vacío, validación, error y éxito. Los ocho módulos pasaron `node --check`; `mvn -DskipTests package` y `docker compose up --build -d` finalizaron correctamente. Compose quedó con app y Adminer activos y MySQL healthy; `/`, `/evento.html`, CSS/JS y `/api/health` respondieron HTTP 200, y los payloads de equipo y asistencia de jugador/entrenador se comprobaron con HTTP 200. URL de revisión: `http://localhost:8080/`. La prueba interactiva completa de navegador/consola queda pendiente: Playwright no está instalado y Edge headless no devolvió un DOM útil; no se inició la Fase 6.

## Cambio A — Sesión y perfil

### Prompt

Quitar el selector de usuario y añadir acceso por email, registro validado, sesión simulada en `localStorage`, cabecera `X-Usuario-Id`, menú de perfil, edición del nombre y páginas de ajustes/guía/ayuda; documentar la limitación, ejecutar `mvn test` y verificar registro, login y logout con curl.

### Decisiones de arquitectura

- La sesión se guarda como un objeto JSON con `usuarioId`, `nombre` y `email`; las peticiones del wrapper `api.js` envían el id en `X-Usuario-Id` cuando hay sesión. Login y registro consultan/crean usuarios sin credenciales, por lo que queda explícito que no es autenticación real.
- El backend reutiliza `UsuarioService`: búsqueda normalizada por email devuelve `404` con indicación de registro, y `PUT /api/usuarios/{id}` cambia solo el nombre del propio usuario, conservando el email y rechazando ediciones de perfiles ajenos.
- La interfaz mantiene un resumen de perfil en el desplegable común y páginas propias para ajustes, los cuatro pasos de uso y preguntas frecuentes en acordeón; las páginas de la aplicación requieren una sesión y la raíz redirige al acceso.

### Resultado

Implementación realizada; `mvn test` terminó en `BUILD SUCCESS` con 55 tests, 0 fallos/errores/omitidos (9 pruebas Testcontainers), y los 13 módulos JavaScript pasaron `node --check`. `docker compose up --build -d` reconstruyó la aplicación; `/api/health`, las cinco páginas nuevas y las consultas de login devolvieron `200`; registro `201` con `Location`, email inexistente `404`, email duplicado `409`, actualización propia `200` y edición de otro perfil `403`. El endpoint de destino `login.html` devuelve `200`; el logout es deliberadamente cliente, borra `localStorage` y redirige, por lo que curl no puede ejecutar esa acción del navegador. Se borraron de MySQL las dos cuentas temporales de prueba creadas durante la comprobación.

## Errores corregidos manualmente por el alumno