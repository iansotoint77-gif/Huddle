package es.huddle.dto;

import es.huddle.model.Rol;

public record EquipoResumenDto(Long id, String nombre, String categoria, Long creadorId, Rol rol,
                               int numeroJugadores, int eventosProximos) {
}