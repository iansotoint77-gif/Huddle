package es.huddle.service;

import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dao.RespuestaDao;
import es.huddle.dao.UsuarioDao;
import es.huddle.dto.GuardarRespuestaDto;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.EstadoAsistencia;
import es.huddle.model.Evento;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Respuesta;
import es.huddle.model.Rol;
import es.huddle.model.TipoEvento;
import es.huddle.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RespuestaServiceTest {
    private EventoDao eventoDao;
    private MiembroDao miembroDao;
    private RespuestaDao respuestaDao;
    private UsuarioDao usuarioDao;
    private RespuestaService respuestaService;

    @BeforeEach
    void setUp() {
        eventoDao = mock(EventoDao.class);
        miembroDao = mock(MiembroDao.class);
        respuestaDao = mock(RespuestaDao.class);
        usuarioDao = mock(UsuarioDao.class);
        respuestaService = new RespuestaService(eventoDao, miembroDao, respuestaDao, usuarioDao);
    }

    @Test
    void guardarRespuesta_votaYPermiteActualizarElVoto() {
        eventoFuturo(10L);
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        var primera = respuestaService.guardarRespuesta(2L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.VOY, "Voy"));
        var segunda = respuestaService.guardarRespuesta(2L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.NO_VOY, null));

        assertEquals(EstadoAsistencia.VOY, primera.estado());
        assertEquals(EstadoAsistencia.NO_VOY, segunda.estado());
        verify(respuestaDao, org.mockito.Mockito.times(2)).upsert(any(Respuesta.class));
    }

    @Test
    void guardarRespuesta_noMiembro_lanzaForbiddenException() {
        eventoFuturo(10L);
        when(miembroDao.findById(3L, 4L)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> respuestaService.guardarRespuesta(4L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.VOY, null)));
        verify(respuestaDao, never()).upsert(any(Respuesta.class));
    }

    @Test
    void guardarRespuesta_entrenadorNoPuedeVotar_lanzaForbiddenException() {
        eventoFuturo(10L);
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));

        assertThrows(ForbiddenException.class, () -> respuestaService.guardarRespuesta(1L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.VOY, null)));
    }

    @Test
    void guardarRespuesta_eventoPasado_lanzaValidationException() {
        when(eventoDao.findById(10L)).thenReturn(Optional.of(new Evento(10L, 3L, TipoEvento.ENTRENAMIENTO,
                "Entrenamiento", LocalDateTime.now().minusDays(1), "Pabellón", null)));
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertThrows(ValidationException.class, () -> respuestaService.guardarRespuesta(2L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.VOY, null)));
    }

    @Test
    void guardarRespuesta_eventoInexistente_lanzaNotFoundExceptionAntesDePermisos() {
        when(eventoDao.findById(10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> respuestaService.guardarRespuesta(2L, 10L,
                new GuardarRespuestaDto(EstadoAsistencia.VOY, null)));
        verify(miembroDao, never()).findById(3L, 2L);
    }

    @Test
    void guardarRespuesta_estadoAusente_lanzaValidationException() {
        eventoFuturo(10L);
        when(miembroDao.findById(3L, 2L)).thenReturn(Optional.of(new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8)));

        assertThrows(ValidationException.class, () -> respuestaService.guardarRespuesta(2L, 10L,
                new GuardarRespuestaDto(null, null)));
    }

    @Test
    void listarRespuestas_incluyeEstadosYJugadoresSinResponder() {
        eventoFuturo(10L);
        when(miembroDao.findById(3L, 1L)).thenReturn(Optional.of(new MiembroEquipo(3L, 1L, Rol.ENTRENADOR, null)));
        when(miembroDao.findByEquipoId(3L)).thenReturn(List.of(
                new MiembroEquipo(3L, 2L, Rol.JUGADOR, 8), new MiembroEquipo(3L, 3L, Rol.JUGADOR, 9),
                new MiembroEquipo(3L, 4L, Rol.JUGADOR, 10), new MiembroEquipo(3L, 5L, Rol.JUGADOR, 11)));
        when(usuarioDao.findById(2L)).thenReturn(Optional.of(new Usuario(2L, "Uno", "uno@example.com")));
        when(usuarioDao.findById(3L)).thenReturn(Optional.of(new Usuario(3L, "Dos", "dos@example.com")));
        when(usuarioDao.findById(4L)).thenReturn(Optional.of(new Usuario(4L, "Tres", "tres@example.com")));
        when(usuarioDao.findById(5L)).thenReturn(Optional.of(new Usuario(5L, "Cuatro", "cuatro@example.com")));
        when(respuestaDao.findByEventoId(10L)).thenReturn(List.of(
                new Respuesta(10L, 2L, EstadoAsistencia.VOY, null, LocalDateTime.now()),
                new Respuesta(10L, 3L, EstadoAsistencia.NO_VOY, null, LocalDateTime.now()),
                new Respuesta(10L, 4L, EstadoAsistencia.QUIZAS, "Quizá", LocalDateTime.now())));

        var resumen = respuestaService.listarRespuestas(1L, 10L);

        assertEquals(1, resumen.voy());
        assertEquals(1, resumen.noVoy());
        assertEquals(1, resumen.quizas());
        assertEquals(1, resumen.sinResponder());
        assertEquals("SIN_RESPONDER", resumen.jugadores().get(resumen.jugadores().size() - 1).estado());
    }

    private void eventoFuturo(long id) {
        when(eventoDao.findById(id)).thenReturn(Optional.of(new Evento(id, 3L, TipoEvento.ENTRENAMIENTO,
                "Entrenamiento", LocalDateTime.now().plusDays(2), "Pabellón", null)));
    }
}