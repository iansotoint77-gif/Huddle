package es.huddle.service;

import es.huddle.dao.UsuarioDao;
import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.dto.UsuarioDto;
import es.huddle.dto.mapper.UsuarioMapper;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Usuario;

import java.util.List;
import java.util.Objects;

public final class UsuarioService {
    private final UsuarioDao usuarioDao;

    public UsuarioService(UsuarioDao usuarioDao) {
        this.usuarioDao = Objects.requireNonNull(usuarioDao);
    }

    public List<UsuarioDto> listarUsuarios() {
        return usuarioDao.findAll().stream().map(UsuarioMapper::toDto).toList();
    }

    public UsuarioDto crearUsuario(GuardarUsuarioDto dto) {
        if (dto == null) {
            throw new ValidationException("Los datos del usuario son obligatorios.");
        }
        String nombre = ServiceValidation.required(dto.nombre(), "nombre", 120);
        String email = ServiceValidation.email(dto.email());
        if (usuarioDao.findByEmail(email).isPresent()) {
            throw new DuplicateException("Ya existe un usuario con ese email.");
        }
        Usuario creado;
        try {
            creado = usuarioDao.insert(new Usuario(null, nombre, email));
        } catch (DuplicateException exception) {
            throw new DuplicateException("Ya existe un usuario con ese email.", exception);
        }
        return UsuarioMapper.toDto(creado);
    }
}