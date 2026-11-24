package es.huddle.service;

import es.huddle.dao.EquipoDao;
import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dto.GuardarEventoDto;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Equipo;
import es.huddle.model.Evento;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Rol;
import es.huddle.model.TipoEvento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventoServiceTest {
    private EquipoDao equipoDao;
    private EventoDao eventoDao;
    private MiembroDao miembroDao;
    private EventoService eventoService;

    @BeforeEach
    void setUp() {
        equipoDao = mock(EquipoDao.class);
        eventoDao = mock(EventoDao.class);
        miembroDao = mock(MiembroDao.class);
        eventoService = new EventoService(equipoDao, eventoDao, miembroDao);
    }

    @Test
    void crearEvento_entrenamientoLimpiaElRivalYDevuelveDto() {
        entrenador();
        when(eventoDao.insert(any(Evento.class))).thenAnswer(invocation -> {
            Evento nuevo = invocation.getArgument(0, Evento.class);
            return new Evento(10L, nuevo.equipoId(), nuevo.tipo(), nuevo.titulo(), nuevo.fechaHora(), nuevo.lugar(),
                    nuevo.rival());
        });

        var creado = eventoService.crearEvento(1L, 3L, entrada(TipoEvento.ENTRENAMIENTO, "rival ignorado"));

        assertEquals(10L, creado.id());
        assertNull(creado.rival());
        verify(eventoDao).insert(org.mockito.ArgumentMatchers.argThat(evento ->
                evento.equipoId() == 3L && evento.tipo() == TipoEvento.ENTRENAMIENTO && evento.rival() == null));
    }

    @Test
    void crearEvento_siNoEsEntrenador_lanzaForbiddenException() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Equipo", "U12", 1L)));
        when(miembroDao.findById(3L, 2L))
                .thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertThrows(ForbiddenException.class,
                () -> eventoService.crearEvento(2L, 3L, entrada(TipoEvento.ENTRENAMIENTO, null)));
        verify(eventoDao, never()).insert(any(Evento.class));
    }

    @Test
    void crearEvento_equipoInexistenteSeCompruebaAntesDelPermiso_lanzaNotFoundException() {
        when(equipoDao.findById(77L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventoService.crearEvento(2L, 77L, entrada(TipoEvento.ENTRENAMIENTO, null)));
        verify(miembroDao, never()).findById(77L, 2L);
    }

    @Test
    void crearEvento_partidoSinRival_lanzaValidationException() {
        entrenador();

        assertThrows(ValidationException.class,
                () -> eventoService.crearEvento(1L, 3L, entrada(TipoEvento.PARTIDO, " ")));
    }

    @Test
    void crearEvento_fechaPasada_lanzaValidationException() {
        entrenador();
        GuardarEventoDto pasado = new GuardarEventoDto(TipoEvento.ENTRENAMIENTO, "Entrenamiento",
                LocalDateTime.now().minusDays(1), "Pabellón", null);

        assertThrows(ValidationException.class, () -> eventoService.crearEvento(1L, 3L, pasado));
    }

    @Test
    void obtenerEvento_noMiembro_lanzaForbiddenException() {
        Evento evento = evento(10L);
        when(eventoDao.findById(10L)).thenReturn(Optional.of(evento));
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Equipo", "U12", 1L)));
        when(miembroDao.findById(3L, 4L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> eventoService.obtenerEvento(4L, 10L));
    }

    @Test
    void obtenerEvento_miembroObtieneElDetalle() {
        when(eventoDao.findById(10L)).thenReturn(Optional.of(evento(10L)));
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Equipo", "U12", 1L)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertEquals(10L, eventoService.obtenerEvento(2L, 10L).id());
    }

    @Test
    void obtenerEvento_inexistente_lanzaNotFoundAntesDePermisos() {
        when(eventoDao.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventoService.obtenerEvento(4L, 10L));
        verify(equipoDao, never()).findById(3L);
    }

    @Test
    void actualizarEvento_entrenadorActualizaElEvento() {
        when(eventoDao.findById(10L)).thenReturn(Optional.of(evento(10L)));
        entrenador();
        when(eventoDao.update(any(Evento.class))).thenReturn(1);

        var actualizado = eventoService.actualizarEvento(1L, 10L,
                new GuardarEventoDto(TipoEvento.PARTIDO, "Partido", LocalDateTime.now().plusDays(3),
                        "Pabellón", "Rival"));

        assertEquals(TipoEvento.PARTIDO, actualizado.tipo());
        assertEquals("Rival", actualizado.rival());
    }

    @Test
    void eliminarEvento_entrenadorBorraElEvento() {
        when(eventoDao.findById(10L)).thenReturn(Optional.of(evento(10L)));
        entrenador();
        when(eventoDao.deleteById(10L)).thenReturn(1);

        eventoService.eliminarEvento(1L, 10L);

        verify(eventoDao).deleteById(10L);
    }

    private void entrenador() {
        when(equipoDao.findById(3L)).thenReturn(Optional.of(new Equipo(3L, "Equipo", "U12", 1L)));
        when(miembroDao.findById(3L, 1L))
                .thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
    }

    private GuardarEventoDto entrada(TipoEvento tipo, String rival) {
        return new GuardarEventoDto(tipo, "Entrenamiento", LocalDateTime.now().plusDays(2).withNano(0),
                "Pabellón", rival);
    }

    private Evento evento(long id) {
        return new Evento(id, 3L, TipoEvento.ENTRENAMIENTO, "Entrenamiento", LocalDateTime.now().plusDays(2),
                "Pabellón", null);
    }
}