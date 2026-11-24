package es.huddle.dto.mapper;

import es.huddle.dto.EventoDto;
import es.huddle.dto.GuardarEventoDto;
import es.huddle.model.Evento;

public final class EventoMapper {
    private EventoMapper() {
    }

    public static EventoDto toDto(Evento evento) {
        return new EventoDto(evento.id(), evento.equipoId(), evento.tipo(), evento.titulo(), evento.fechaHora(),
                evento.lugar(), evento.rival());
    }

    public static Evento toModel(Long id, Long equipoId, GuardarEventoDto dto) {
        return new Evento(id, equipoId, dto.tipo(), dto.titulo(), dto.fechaHora(), dto.lugar(), dto.rival());
    }
}