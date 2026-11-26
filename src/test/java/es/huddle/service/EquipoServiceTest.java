package es.huddle.service;

import es.huddle.dao.EquipoDao;
import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dao.RespuestaDao;
import es.huddle.dao.UsuarioDao;
import es.huddle.dto.AnadirMiembroDto;
import es.huddle.dto.ActualizarDorsalDto;
import es.huddle.dto.GuardarEquipoDto;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Equipo;
import es.huddle.model.Evento;
import es.huddle.model.EstadoAsistencia;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Respuesta;
import es.huddle.model.Rol;
import es.huddle.model.TipoEvento;
import es.huddle.model.Usuario;
import es.huddle.util.TransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EquipoServiceTest {
    private UsuarioDao usuarioDao;
    private EquipoDao equipoDao;
    private MiembroDao miembroDao;
    private EventoDao eventoDao;
    private RespuestaDao respuestaDao;
    private TransactionManager transactionManager;
    private List<String> transaccion;
    private EquipoService equipoService;

    @BeforeEach
    void setUp() {
        usuarioDao = mock(UsuarioDao.class);
        equipoDao = mock(EquipoDao.class);
        miembroDao = mock(MiembroDao.class);
        eventoDao = mock(EventoDao.class);
        respuestaDao = mock(RespuestaDao.class);
        transaccion = new ArrayList<>();
        transactionManager = new TransactionManager(new TrackingDataSource(transaccion));
        equipoService = new EquipoService(usuarioDao, equipoDao, miembroDao, eventoDao, respuestaDao,
                transactionManager);
    }

    @Test
    void crearEquipo_insertaEquipoYEntrenadorEnUnaTransaccion() {
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Carlos", "carlos@example.com")));
        when(equipoDao.insert(any(Equipo.class))).thenReturn(new Equipo(8L, "Minibasket", "U12", 1L));
        when(miembroDao.insert(any(MiembroEquipo.class))).thenReturn(1);

        var creado = equipoService.crearEquipo(1L, new GuardarEquipoDto("Minibasket", "U12"));

        assertEquals(8L, creado.id());
        assertTrue(transaccion.contains("autocommit:false"));
        assertTrue(transaccion.contains("commit"));
        verify(miembroDao).insert(new MiembroEquipo(8L, 1L, Rol.ENTRENADOR, null));
    }

    @Test
    void crearEquipo_fallaLaSegundaInsercion_revierteLaTransaccion() {
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Carlos", "carlos@example.com")));
        when(equipoDao.insert(any(Equipo.class))).thenReturn(new Equipo(8L, "Minibasket", "U12", 1L));
        when(miembroDao.insert(any(MiembroEquipo.class))).thenThrow(new RuntimeException("fallo"));

        assertThrows(RuntimeException.class,
                () -> equipoService.crearEquipo(1L, new GuardarEquipoDto("Minibasket", "U12")));

        assertTrue(transaccion.contains("rollback"));
        org.junit.jupiter.api.Assertions.assertFalse(transaccion.contains("commit"));
    }

    @Test
    void crearEquipo_usuarioInexistente_lanzaNotFoundException() {
        when(usuarioDao.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> equipoService.crearEquipo(99L, new GuardarEquipoDto("Equipo", "U12")));
    }

    @Test
    void actualizarEquipo_entrenadorActualizaDatosYConservaCreador() {
        entrenador(3L, 1L);
        when(equipoDao.update(any(Equipo.class))).thenReturn(1);

        var actualizado = equipoService.actualizarEquipo(1L, 3L, new GuardarEquipoDto("Minibasket", "Cadete"));

        assertEquals("Minibasket", actualizado.nombre());
        assertEquals("Cadete", actualizado.categoria());
        verify(equipoDao).update(new Equipo(3L, "Minibasket", "Cadete", 1L));
    }

    @Test
    void actualizarEquipo_jugadorNoPuedeEditar_lanzaForbiddenException() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertThrows(ForbiddenException.class,
                () -> equipoService.actualizarEquipo(2L, 3L, new GuardarEquipoDto("Editado", "U12")));
        verify(equipoDao, never()).update(any(Equipo.class));
    }

    @Test
    void eliminarEquipo_entrenadorBorraElEquipo() {
        entrenador(3L, 1L);
        when(equipoDao.deleteById(3L)).thenReturn(1);

        equipoService.eliminarEquipo(1L, 3L);

        verify(equipoDao).deleteById(3L);
    }

    @Test
    void eliminarEquipo_jugadorNoPuedeBorrar_lanzaForbiddenException() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertThrows(ForbiddenException.class, () -> equipoService.eliminarEquipo(2L, 3L));
        verify(equipoDao, never()).deleteById(3L);
    }

    @Test
    void listarEquipos_devuelveRolYTotalesPorEquipo() {
        Equipo equipo = new Equipo(3L, "Minibasket", "U12", 1L);
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Carlos", "carlos@example.com")));
        when(equipoDao.findByUsuarioId(1L)).thenReturn(List.of(equipo));
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(
                new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null),
                new MiembroEquipo(3L, 2L, Rol.JUGADOR, 9)));
        when(eventoDao.findByEquipoId(3L)).thenReturn(List.of(
                evento(20L, LocalDateTime.now().plusDays(1)), evento(21L, LocalDateTime.now().minusDays(1))));

        var resumen = equipoService.listarEquipos(1L).get(0);

        assertEquals(Rol.ENTRENADOR, resumen.rol());
        assertEquals(1, resumen.numeroJugadores());
        assertEquals(1, resumen.eventosProximos());
    }

    @Test
    void obtenerEquipo_equipoInexistente_antesDeComprobarPermiso_lanzaNotFoundException() {
        when(equipoDao.findById(77L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> equipoService.obtenerEquipo(2L, 77L));
        verify(miembroDao, never()).findById(77L, 2L);
    }

    @Test
    void obtenerEquipo_miembroObtieneDetalleConSuRol() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        var detalle = equipoService.obtenerEquipo(2L, 3L);

        assertEquals("Minibasket", detalle.equipo().nombre());
        assertEquals(Rol.JUGADOR, detalle.rol());
    }

    @Test
    void listarMiembros_miembroDelEquipoObtieneLaPlantilla() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(
                new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null), new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Carlos", "carlos@example.com")));
        when(usuarioDao.findById(2L)).thenReturn(Optional.of(new Usuario(2L, "Lucas", "lucas@example.com")));

        var plantilla = equipoService.listarMiembros(2L, 3L);

        assertEquals(2, plantilla.size());
        assertEquals(8, plantilla.get(1).dorsal());
        assertEquals("Lucas", plantilla.get(1).nombre());
    }

    @Test
    void agregarJugador_agregaUsuarioComoJugador() {
        entrenador(3L, 1L);
        when(usuarioDao.findByEmail("lucas@example.com"))
                .thenReturn(Optional.of(new Usuario(2L, "Lucas", "lucas@example.com")));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
        when(miembroDao.insert(any(MiembroEquipo.class))).thenReturn(1);

        var miembro = equipoService.agregarJugador(1L, 3L, new AnadirMiembroDto("lucas@example.com", 8));

        assertEquals(Rol.JUGADOR, miembro.rol());
        assertEquals(8, miembro.dorsal());
        verify(miembroDao).insert(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8));
    }

    @Test
    void agregarJugador_dorsalDuplicado_lanzaDuplicateException() {
        entrenador(3L, 1L);
        when(usuarioDao.findByEmail("lucas@example.com"))
                .thenReturn(Optional.of(new Usuario(2L, "Lucas", "lucas@example.com")));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(new MiembroEquipo(3L, 4L, Rol.JUGADOR, 8)));

        assertThrows(DuplicateException.class,
                () -> equipoService.agregarJugador(1L, 3L, new AnadirMiembroDto("lucas@example.com", 8)));
        verify(miembroDao, never()).insert(any(MiembroEquipo.class));
    }

    @Test
    void agregarJugador_usuarioNoEncontrado_lanzaNotFoundException() {
        entrenador(3L, 1L);
        when(usuarioDao.findByEmail("lucas@example.com")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> equipoService.agregarJugador(1L, 3L, new AnadirMiembroDto("lucas@example.com", 8)));
    }

    @Test
    void agregarJugador_dorsalFueraDeRango_lanzaValidationException() {
        entrenador(3L, 1L);

        assertThrows(ValidationException.class,
                () -> equipoService.agregarJugador(1L, 3L, new AnadirMiembroDto("lucas@example.com", 100)));
    }

    @Test
    void cambiarDorsal_actualizaElDorsalDeUnMiembro() {
        entrenador(3L, 1L);
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(miembroDao.updateDorsal(3L, 2L, 9)).thenReturn(1);

        assertEquals(9, equipoService.cambiarDorsal(1L, 3L, 2L, new ActualizarDorsalDto(9)).dorsal());
    }

    @Test
    void cambiarDorsal_dorsalDuplicado_lanzaDuplicateException() {
        entrenador(3L, 1L);
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(new MiembroEquipo(3L, 3L, Rol.JUGADOR, 9)));

        assertThrows(DuplicateException.class,
                () -> equipoService.cambiarDorsal(1L, 3L, 2L, new ActualizarDorsalDto(9)));
    }

    @Test
    void eliminarMiembro_noPermiteEliminarAlCreador() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.ENTRENADOR, null)));
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));

        assertThrows(ValidationException.class, () -> equipoService.eliminarMiembro(2L, 3L, 1L));
        verify(miembroDao, never()).deleteById(3L, 1L);
    }

    @Test
    void eliminarMiembro_entrenadorPuedeEliminarJugador() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));
        when(miembroDao.deleteById(3L, 2L)).thenReturn(1);

        equipoService.eliminarMiembro(1L, 3L, 2L);

        verify(miembroDao).deleteById(3L, 2L);
    }

    @Test
    void listarEventos_filtraPorTiempoYResumeLasRespuestasDeJugadores() {
        Equipo equipo = new Equipo(3L, "Minibasket", "U12", 1L);
        Evento futuro = evento(20L, LocalDateTime.now().plusDays(2));
        when(equipoDao.findById(3L)).thenReturn(Optional.of(equipo));
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8),
                new MiembroEquipo(3L, 3L, Rol.JUGADOR, 9)));
        when(eventoDao.findByEquipoId(3L)).thenReturn(List.of(futuro));
        when(respuestaDao.findByEventoId(20L)).thenReturn(List.of(
                new Respuesta(20L, 2L, EstadoAsistencia.VOY, null, LocalDateTime.now())));

        var resumen = equipoService.listarEventos(1L, 3L, "proximos").get(0);

        assertEquals(1, resumen.voy());
        assertEquals(0, resumen.noVoy());
        assertEquals(1, resumen.sinResponder());
    }

    @Test
    void crearEquipo_nombreVacio_lanzaValidationException() {
        assertThrows(ValidationException.class,
                () -> equipoService.crearEquipo(1L, new GuardarEquipoDto(" ", "U12")));
    }

    private void entrenador(long equipoId, long usuarioId) {
        when(equipoDao.findById(equipoId)).thenReturn(Optional.of(new Equipo(equipoId, "Minibasket", "U12", 1L)));
        when(miembroDao.findById(equipoId, usuarioId))
                .thenReturn(Optional.of(new MiembroEquipo(equipoId, usuarioId, Rol.ENTRENADOR, null)));
    }

    private Evento evento(long id, LocalDateTime fechaHora) {
        return new Evento(id, 3L, TipoEvento.ENTRENAMIENTO, "Entrenamiento", fechaHora, "Pabellón", null);
    }

    private static final class TrackingDataSource implements DataSource {
        private final List<String> calls;

        private TrackingDataSource(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public Connection getConnection() {
            return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "setAutoCommit" -> calls.add("autocommit:" + args[0]);
                            case "commit" -> calls.add("commit");
                            case "rollback" -> calls.add("rollback");
                            default -> {
                            }
                        }
                        return null;
                    });
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("No compatible wrapper.");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}