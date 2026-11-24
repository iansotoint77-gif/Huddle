package es.huddle.dto.mapper;

import es.huddle.dto.EquipoDto;
import es.huddle.dto.GuardarEquipoDto;
import es.huddle.model.Equipo;

public final class EquipoMapper {
    private EquipoMapper() {
    }

    public static EquipoDto toDto(Equipo equipo) {
        return new EquipoDto(equipo.id(), equipo.nombre(), equipo.categoria(), equipo.creadorId());
    }

    public static Equipo toModel(GuardarEquipoDto dto, Long creadorId) {
        return new Equipo(null, dto.nombre(), dto.categoria(), creadorId);
    }
}