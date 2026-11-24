package es.huddle.dto;

import es.huddle.model.EstadoAsistencia;

public record GuardarRespuestaDto(EstadoAsistencia estado, String comentario) {
}