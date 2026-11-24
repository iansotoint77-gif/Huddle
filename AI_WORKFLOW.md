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

## Errores corregidos manualmente por el alumno