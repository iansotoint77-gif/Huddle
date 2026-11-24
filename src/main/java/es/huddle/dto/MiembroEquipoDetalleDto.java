package es.huddle.dto;

import es.huddle.model.Rol;

public record MiembroEquipoDetalleDto(Long equipoId, Long usuarioId, String nombre, String email, Rol rol,
                                      Integer dorsal) {
}