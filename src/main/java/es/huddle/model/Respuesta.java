package es.huddle.model;

import java.time.LocalDateTime;

public record Respuesta(Long eventoId, Long usuarioId, EstadoAsistencia estado, String comentario,
                        LocalDateTime fechaRespuesta) {
}