package es.huddle.dto;

import es.huddle.model.TipoEvento;

import java.time.LocalDateTime;

public record EventoDto(Long id, Long equipoId, TipoEvento tipo, String titulo, LocalDateTime fechaHora,
                        String lugar, String rival) {
}