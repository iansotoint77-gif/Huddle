package es.huddle.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import es.huddle.exception.DataAccessException;
import es.huddle.exception.DuplicateException;
import es.huddle.model.Equipo;
import es.huddle.model.EstadoAsistencia;
import es.huddle.model.Evento;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Respuesta;
import es.huddle.model.Rol;
import es.huddle.model.TipoEvento;
import es.huddle.model.Usuario;
import es.huddle.util.TransactionManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class DaoIntegrationTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("huddle_test")
            .withUsername("huddle_test")
            .withPassword("huddle_test")
            .withInitScript("db/init.sql");

    private static HikariDataSource dataSource;
    private static TransactionManager transactionManager;
    private static UsuarioDaoJdbc usuarioDao;
    private static EquipoDaoJdbc equipoDao;
    private static MiembroDaoJdbc miembroDao;
    private static EventoDaoJdbc eventoDao;
    private static RespuestaDaoJdbc respuestaDao;

    @BeforeAll
    static void crearDaos() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl());
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        config.setMaximumPoolSize(3);
        dataSource = new HikariDataSource(config);
        transactionManager = new TransactionManager(dataSource);
        usuarioDao = new UsuarioDaoJdbc(transactionManager);
        equipoDao = new EquipoDaoJdbc(transactionManager);
        miembroDao = new MiembroDaoJdbc(transactionManager);
        eventoDao = new EventoDaoJdbc(transactionManager);
        respuestaDao = new RespuestaDaoJdbc(transactionManager);
    }

    @AfterAll
    static void cerrarDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Test
    void usuarioDao_realizaCrudYDevuelveVacioParaIdInexistente() {
        String email = emailUnico();
        Usuario creado = usuarioDao.insert(new Usuario(null, "Prueba DAO", email));

        assertNotNull(creado.id());
        assertEquals(creado, usuarioDao.findById(creado.id()).orElseThrow());
        assertEquals(creado, usuarioDao.findByEmail(email).orElseThrow());
        assertTrue(usuarioDao.findAll().contains(creado));

        Usuario actualizado = new Usuario(creado.id(), "Usuario editado", "editado-" + email);
        assertEquals(1, usuarioDao.update(actualizado));
        assertEquals(actualizado, usuarioDao.findById(creado.id()).orElseThrow());
        assertEquals(1, usuarioDao.deleteById(creado.id()));
        assertFalse(usuarioDao.findById(creado.id()).isPresent());
        assertEquals(0, usuarioDao.deleteById(creado.id()));
    }

    @Test
    void usuarioDao_emailDuplicadoLanzaDuplicateException() {
        assertThrows(DuplicateException.class,
                () -> usuarioDao.insert(new Usuario(null, "Duplicado", "carlos.ruiz@example.com")));
    }

    @Test
    void equipoDao_realizaCrudYConsultaPorCreadorYMiembro() {
        Usuario creador = crearUsuario();
        Equipo creado = equipoDao.insert(new Equipo(null, "Equipo DAO", "Cadete", creador.id()));

        assertNotNull(creado.id());
        assertEquals(creado, equipoDao.findById(creado.id()).orElseThrow());
        assertTrue(equipoDao.findByCreadorId(creador.id()).contains(creado));
        assertTrue(equipoDao.findByUsuarioId(1L).stream().anyMatch(equipo -> equipo.id().equals(1L)));
        assertTrue(equipoDao.findAll().contains(creado));

        Equipo actualizado = new Equipo(creado.id(), "Equipo actualizado", "Junior", creador.id());
        assertEquals(1, equipoDao.update(actualizado));
        assertEquals(actualizado, equipoDao.findById(creado.id()).orElseThrow());
        assertEquals(1, equipoDao.deleteById(creado.id()));
        assertFalse(equipoDao.findById(creado.id()).isPresent());
        assertEquals(0, equipoDao.deleteById(creado.id()));
        usuarioDao.deleteById(creador.id());
    }

    @Test
    void miembroDao_realizaCrudYControlaDorsalDuplicado() {
        Usuario primero = crearUsuario();
        Usuario segundo = crearUsuario();
        MiembroEquipo miembro = new MiembroEquipo(1L, primero.id(), Rol.JUGADOR, 90);

        assertEquals(1, miembroDao.insert(miembro));
        assertEquals(miembro, miembroDao.findById(1L, primero.id()).orElseThrow());
        assertTrue(miembroDao.existsById(1L, primero.id()));
        assertTrue(miembroDao.findByEquipoId(1L).contains(miembro));
        assertTrue(miembroDao.findByUsuarioId(primero.id()).contains(miembro));
        assertThrows(DuplicateException.class,
                () -> miembroDao.insert(new MiembroEquipo(1L, segundo.id(), Rol.JUGADOR, 90)));

        assertEquals(1, miembroDao.updateDorsal(1L, primero.id(), 99));
        assertEquals(99, miembroDao.findById(1L, primero.id()).orElseThrow().dorsal());
        assertEquals(1, miembroDao.deleteById(1L, primero.id()));
        assertFalse(miembroDao.existsById(1L, primero.id()));
        usuarioDao.deleteById(primero.id());
        usuarioDao.deleteById(segundo.id());
    }

    @Test
    void eventoDao_realizaCrudYElBorradoEliminaRespuestasEnCascada() {
        Usuario jugador = crearUsuario();
        Evento creado = eventoDao.insert(eventoNuevo(1L, "Evento DAO"));

        assertNotNull(creado.id());
        assertEquals(creado, eventoDao.findById(creado.id()).orElseThrow());
        assertTrue(eventoDao.findByEquipoId(1L).contains(creado));
        assertTrue(eventoDao.findAll().contains(creado));

        Evento actualizado = new Evento(creado.id(), 1L, TipoEvento.PARTIDO, "Partido DAO",
                creado.fechaHora().plusDays(1), "Pabellón DAO", "Rival DAO");
        assertEquals(1, eventoDao.update(actualizado));
        assertEquals(actualizado, eventoDao.findById(creado.id()).orElseThrow());
        respuestaDao.upsert(respuesta(creado.id(), jugador.id(), EstadoAsistencia.VOY, "Confirmado"));

        assertEquals(1, eventoDao.deleteById(creado.id()));
        assertFalse(eventoDao.findById(creado.id()).isPresent());
        assertFalse(respuestaDao.findById(creado.id(), jugador.id()).isPresent());
        assertEquals(0, eventoDao.deleteById(creado.id()));
        usuarioDao.deleteById(jugador.id());
    }

    @Test
    void respuestaDao_haceUpsertConsultaYBorrado() {
        Usuario jugador = crearUsuario();
        LocalDateTime primeraFecha = LocalDateTime.now().withNano(0);
        Respuesta primera = new Respuesta(1L, jugador.id(), EstadoAsistencia.VOY, "Voy", primeraFecha);
        assertEquals(1, respuestaDao.upsert(primera));
        assertEquals(primera, respuestaDao.findById(1L, jugador.id()).orElseThrow());
        assertTrue(respuestaDao.findByEventoId(1L).contains(primera));
        assertTrue(respuestaDao.findByUsuarioId(jugador.id()).contains(primera));

        Respuesta actualizada = new Respuesta(1L, jugador.id(), EstadoAsistencia.NO_VOY, null,
                primeraFecha.plusMinutes(1));
        assertEquals(2, respuestaDao.upsert(actualizada));
        assertEquals(actualizada, respuestaDao.findById(1L, jugador.id()).orElseThrow());
        assertEquals(1, respuestaDao.deleteById(1L, jugador.id()));
        assertFalse(respuestaDao.findById(1L, jugador.id()).isPresent());
        assertEquals(0, respuestaDao.deleteById(1L, jugador.id()));
        usuarioDao.deleteById(jugador.id());
    }

    @Test
    void respuestaDao_violacionDeClaveForaneaLanzaDataAccessException() {
        Respuesta respuesta = new Respuesta(Long.MAX_VALUE, 1L, EstadoAsistencia.VOY, null,
                LocalDateTime.now().withNano(0));

        assertThrows(DataAccessException.class, () -> respuestaDao.upsert(respuesta));
    }

    @Test
    void dao_registroInexistenteDevuelveVacioOCeroFilasAfectadas() {
        long idInexistente = Long.MAX_VALUE;

        assertFalse(usuarioDao.findById(idInexistente).isPresent());
        assertEquals(0, usuarioDao.update(new Usuario(idInexistente, "Inexistente", emailUnico())));
        assertEquals(0, usuarioDao.deleteById(idInexistente));

        assertFalse(equipoDao.findById(idInexistente).isPresent());
        assertEquals(0, equipoDao.update(new Equipo(idInexistente, "Inexistente", "Cadete", 1L)));
        assertEquals(0, equipoDao.deleteById(idInexistente));

        assertFalse(miembroDao.findById(1L, idInexistente).isPresent());
        assertFalse(miembroDao.existsById(1L, idInexistente));
        assertEquals(0, miembroDao.updateDorsal(1L, idInexistente, 99));
        assertEquals(0, miembroDao.deleteById(1L, idInexistente));

        assertFalse(eventoDao.findById(idInexistente).isPresent());
        assertEquals(0, eventoDao.update(new Evento(idInexistente, 1L, TipoEvento.ENTRENAMIENTO,
                "Inexistente", LocalDateTime.now().plusDays(1).withNano(0), "Pabellón", null)));
        assertEquals(0, eventoDao.deleteById(idInexistente));

        assertFalse(respuestaDao.findById(1L, idInexistente).isPresent());
        assertEquals(0, respuestaDao.deleteById(1L, idInexistente));
    }

    @Test
    void transactionManager_revierteLosCambiosDeUnaTransaccion() {
        String email = emailUnico();
        transactionManager.begin();
        Usuario creado = usuarioDao.insert(new Usuario(null, "Rollback", email));
        transactionManager.rollback();

        assertFalse(usuarioDao.findById(creado.id()).isPresent());
    }

    private Usuario crearUsuario() {
        return usuarioDao.insert(new Usuario(null, "Usuario de prueba", emailUnico()));
    }

    private String emailUnico() {
        return "dao-" + UUID.randomUUID() + "@example.com";
    }

    private Evento eventoNuevo(Long equipoId, String titulo) {
        return new Evento(null, equipoId, TipoEvento.ENTRENAMIENTO, titulo,
                LocalDateTime.now().plusDays(14).withNano(0), "Pabellón de pruebas", null);
    }

    private Respuesta respuesta(Long eventoId, Long usuarioId, EstadoAsistencia estado, String comentario) {
        return new Respuesta(eventoId, usuarioId, estado, comentario, LocalDateTime.now().withNano(0));
    }
}