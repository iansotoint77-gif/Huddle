package es.huddle.dto.mapper;

import es.huddle.dto.GuardarRespuestaDto;
import es.huddle.dto.RespuestaDto;
import es.huddle.model.Respuesta;

import java.time.LocalDateTime;

public final class RespuestaMapper {
    private RespuestaMapper() {
    }

    public static RespuestaDto toDto(Respuesta respuesta) {
        return new RespuestaDto(respuesta.eventoId(), respuesta.usuarioId(), respuesta.estado(),
                respuesta.comentario(), respuesta.fechaRespuesta());
    }

    public static Respuesta toModel(Long eventoId, Long usuarioId, GuardarRespuestaDto dto,
                                    LocalDateTime fechaRespuesta) {
        return new Respuesta(eventoId, usuarioId, dto.estado(), dto.comentario(), fechaRespuesta);
    }
}