package es.huddle.dto;

import es.huddle.model.EstadoAsistencia;

import java.time.LocalDateTime;

public record RespuestaDto(Long eventoId, Long usuarioId, EstadoAsistencia estado, String comentario,
                           LocalDateTime fechaRespuesta) {
}