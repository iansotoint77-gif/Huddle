package es.huddle.model;

import java.time.LocalDateTime;

public record Evento(Long id, Long equipoId, TipoEvento tipo, String titulo, LocalDateTime fechaHora,
                     String lugar, String rival) {
}