package es.huddle.service;

import es.huddle.dao.EquipoDao;
import es.huddle.dao.EventoDao;
import es.huddle.dao.MiembroDao;
import es.huddle.dto.EventoDto;
import es.huddle.dto.GuardarEventoDto;
import es.huddle.dto.mapper.EventoMapper;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Equipo;
import es.huddle.model.Evento;
import es.huddle.model.Rol;
import es.huddle.model.TipoEvento;

import java.time.LocalDateTime;
import java.util.Objects;

public final class EventoService {
    private final EquipoDao equipoDao;
    private final EventoDao eventoDao;
    private final MiembroDao miembroDao;

    public EventoService(EquipoDao equipoDao, EventoDao eventoDao, MiembroDao miembroDao) {
        this.equipoDao = Objects.requireNonNull(equipoDao);
        this.eventoDao = Objects.requireNonNull(eventoDao);
        this.miembroDao = Objects.requireNonNull(miembroDao);
    }

    public EventoDto crearEvento(long usuarioId, long equipoId, GuardarEventoDto dto) {
        Equipo equipo = requireTrainer(equipoId, usuarioId);
        Evento evento = validarYMapear(null, equipo.id(), dto);
        return EventoMapper.toDto(eventoDao.insert(evento));
    }

    public EventoDto obtenerEvento(long usuarioId, long eventoId) {
        Evento evento = eventoDao.findById(eventoId)
                .orElseThrow(() -> new NotFoundException("No existe el evento."));
        if (equipoDao.findById(evento.equipoId()).isEmpty()) {
            throw new NotFoundException("No existe el equipo del evento.");
        }
        if (miembroDao.findById(evento.equipoId(), usuarioId).isEmpty()) {
            throw new ForbiddenException("No perteneces al equipo del evento.");
        }
        return EventoMapper.toDto(evento);
    }

    public EventoDto actualizarEvento(long usuarioId, long eventoId, GuardarEventoDto dto) {
        Evento existente = eventoDao.findById(eventoId)
                .orElseThrow(() -> new NotFoundException("No existe el evento."));
        requireTrainer(existente.equipoId(), usuarioId);
        Evento actualizado = validarYMapear(eventoId, existente.equipoId(), dto);
        if (eventoDao.update(actualizado) == 0 && eventoDao.findById(eventoId).isEmpty()) {
            throw new NotFoundException("No existe el evento.");
        }
        return EventoMapper.toDto(actualizado);
    }

    public void eliminarEvento(long usuarioId, long eventoId) {
        Evento evento = eventoDao.findById(eventoId)
                .orElseThrow(() -> new NotFoundException("No existe el evento."));
        requireTrainer(evento.equipoId(), usuarioId);
        if (eventoDao.deleteById(eventoId) == 0) {
            throw new NotFoundException("No existe el evento.");
        }
    }

    private Equipo requireTrainer(long equipoId, long usuarioId) {
        Equipo equipo = equipoDao.findById(equipoId)
                .orElseThrow(() -> new NotFoundException("No existe el equipo."));
        var miembro = miembroDao.findById(equipoId, usuarioId)
                .orElseThrow(() -> new ForbiddenException("No perteneces a este equipo."));
        if (miembro.rol() != Rol.ENTRENADOR) {
            throw new ForbiddenException("Solo el entrenador puede gestionar eventos.");
        }
        return equipo;
    }

    private Evento validarYMapear(Long eventoId, long equipoId, GuardarEventoDto dto) {
        if (dto == null || dto.tipo() == null) {
            throw new ValidationException("El tipo de evento es obligatorio.");
        }
        String titulo = ServiceValidation.required(dto.titulo(), "título", 160);
        String lugar = ServiceValidation.required(dto.lugar(), "lugar", 160);
        if (dto.fechaHora() == null || !dto.fechaHora().isAfter(LocalDateTime.now())) {
            throw new ValidationException("La fecha del evento debe ser futura.");
        }
        String rival = null;
        if (dto.tipo() == TipoEvento.PARTIDO) {
            rival = ServiceValidation.required(dto.rival(), "rival", 120);
        }
        return EventoMapper.toModel(eventoId, equipoId,
                new GuardarEventoDto(dto.tipo(), titulo, dto.fechaHora(), lugar, rival));
    }
}