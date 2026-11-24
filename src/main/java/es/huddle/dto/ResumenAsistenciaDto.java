package es.huddle.dto;

import java.util.List;

public record ResumenAsistenciaDto(Long eventoId, int voy, int noVoy, int quizas, int sinResponder,
                                   List<JugadorAsistenciaDto> jugadores) {
}