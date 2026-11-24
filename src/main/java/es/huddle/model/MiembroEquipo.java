package es.huddle.model;

public record MiembroEquipo(Long equipoId, Long usuarioId, Rol rol, Integer dorsal) {
}