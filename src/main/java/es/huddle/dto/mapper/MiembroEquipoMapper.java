package es.huddle.dto.mapper;

import es.huddle.dto.AnadirMiembroDto;
import es.huddle.dto.MiembroEquipoDto;
import es.huddle.model.MiembroEquipo;
import es.huddle.model.Rol;

public final class MiembroEquipoMapper {
    private MiembroEquipoMapper() {
    }

    public static MiembroEquipoDto toDto(MiembroEquipo miembro) {
        return new MiembroEquipoDto(miembro.equipoId(), miembro.usuarioId(), miembro.rol(), miembro.dorsal());
    }

    public static MiembroEquipo toModel(Long equipoId, Long usuarioId, Rol rol, AnadirMiembroDto dto) {
        return new MiembroEquipo(equipoId, usuarioId, rol, dto.dorsal());
    }
}