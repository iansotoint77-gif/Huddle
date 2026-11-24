package es.huddle.dto.mapper;

import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.dto.UsuarioDto;
import es.huddle.model.Usuario;

public final class UsuarioMapper {
    private UsuarioMapper() {
    }

    public static UsuarioDto toDto(Usuario usuario) {
        return new UsuarioDto(usuario.id(), usuario.nombre(), usuario.email());
    }

    public static Usuario toModel(GuardarUsuarioDto dto) {
        return new Usuario(null, dto.nombre(), dto.email());
    }
}