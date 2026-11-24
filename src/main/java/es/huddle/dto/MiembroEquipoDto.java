package es.huddle.dto;

import es.huddle.model.Rol;

public record MiembroEquipoDto(Long equipoId, Long usuarioId, Rol rol, Integer dorsal) {
}