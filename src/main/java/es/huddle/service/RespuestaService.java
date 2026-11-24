package es.huddle.service;

import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dao.RespuestaDao;
import es.huddle.dao.UsuarioDao;
import es.huddle.dto.GuardarRespuestaDto;
import es.huddle.dto.JugadorAsistenciaDto;
import es.huddle.dto.RespuestaDto;
import es.huddle.dto.ResumenAsistenciaDto;
import es.huddle.dto.mapper.RespuestaMapper;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.EstadoAsistencia;
import es.huddle.model.Evento;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Respuesta;
import es.huddle.model.Rol;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class RespuestaService {
    private final EventoDao eventoDao;
    private final MiembroDao miembroDao;
    private final RespuestaDao respuestaDao;
    private final UsuarioDao usuarioDao;

    public RespuestaService(EventoDao eventoDao, MiembroDao miembroDao, RespuestaDao respuestaDao,
                            UsuarioDao usuarioDao) {
        this.eventoDao = Objects.requireNonNull(eventoDao);
        this.miembroDao = Objects.requireNonNull(miembroDao);
        this.respuestaDao = Objects.requireNonNull(respuestaDao);
        this.usuarioDao = Objects.requireNonNull(usuarioDao);
    }

    public RespuestaDto guardarRespuesta(long usuarioId, long eventoId, GuardarRespuestaDto dto) {
        Evento evento = obtenerEvento(eventoId);
        MiembroEquipo miembro = miembroDao.findById(evento.equipoId(), usuarioId)
                .orElseThrow(() -> new ForbiddenException("No perteneces al equipo del evento."));
        if (miembro.rol() != Rol.JUGADOR) {
            throw new ForbiddenException("Solo los jugadores pueden responder al evento.");
        }
        if (dto == null || dto.estado() == null) {
            throw new ValidationException("El estado de asistencia es obligatorio.");
        }
        if (evento.fechaHora().isBefore(LocalDateTime.now())) {
            throw new ValidationException("No se puede votar en un evento pasado.");
        }
        String comentario = dto.comentario();
        if (comentario != null && comentario.length() > 500) {
            throw new ValidationException("El comentario no puede superar 500 caracteres.");
        }
        if (comentario != null && comentario.isBlank()) {
            comentario = null;
        }
        Respuesta respuesta = RespuestaMapper.toModel(eventoId, usuarioId,
                new GuardarRespuestaDto(dto.estado(), comentario), LocalDateTime.now());
        respuestaDao.upsert(respuesta);
        return RespuestaMapper.toDto(respuesta);
    }

    public ResumenAsistenciaDto listarRespuestas(long usuarioId, long eventoId) {
        Evento evento = obtenerEvento(eventoId);
        miembroDao.findById(evento.equipoId(), usuarioId)
                .orElseThrow(() -> new ForbiddenException("No perteneces al equipo del evento."));
        List<MiembroEquipo> jugadores = miembroDao.findByEquipoId(evento.equipoId()).stream()
                .filter(miembro -> miembro.rol() == Rol.JUGADOR).toList();
        Map<Long, Respuesta> respuestas = respuestaDao.findByEventoId(eventoId).stream()
                .collect(Collectors.toMap(Respuesta::usuarioId, Function.identity(), (primera, segunda) -> segunda));
        List<JugadorAsistenciaDto> detalle = jugadores.stream().map(jugador -> {
            var usuario = usuarioDao.findById(jugador.usuarioId())
                    .orElseThrow(() -> new NotFoundException("No existe un usuario de la plantilla."));
            Respuesta respuesta = respuestas.get(jugador.usuarioId());
            return new JugadorAsistenciaDto(usuario.id(), usuario.nombre(), jugador.dorsal(),
                    respuesta == null ? "SIN_RESPONDER" : respuesta.estado().name(),
                    respuesta == null ? null : respuesta.comentario());
        }).toList();
        int voy = contar(detalle, EstadoAsistencia.VOY);
        int noVoy = contar(detalle, EstadoAsistencia.NO_VOY);
        int quizas = contar(detalle, EstadoAsistencia.QUIZAS);
        return new ResumenAsistenciaDto(eventoId, voy, noVoy, quizas,
                (int) detalle.stream().filter(jugador -> "SIN_RESPONDER".equals(jugador.estado())).count(), detalle);
    }

    private Evento obtenerEvento(long eventoId) {
        return eventoDao.findById(eventoId).orElseThrow(() -> new NotFoundException("No existe el evento."));
    }

    private int contar(List<JugadorAsistenciaDto> jugadores, EstadoAsistencia estado) {
        return (int) jugadores.stream().filter(jugador -> estado.name().equals(jugador.estado())).count();
    }
}