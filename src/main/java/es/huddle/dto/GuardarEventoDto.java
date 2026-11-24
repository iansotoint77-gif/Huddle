package es.huddle.dto;

import es.huddle.model.TipoEvento;

import java.time.LocalDateTime;

public record GuardarEventoDto(TipoEvento tipo, String titulo, LocalDateTime fechaHora, String lugar, String rival) {
}