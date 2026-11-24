package es.huddle.service;

import es.huddle.dao.UsuarioDao;
import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ValidationException;
import es.huddle.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {
    private UsuarioDao usuarioDao;
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioDao = mock(UsuarioDao.class);
        usuarioService = new UsuarioService(usuarioDao);
    }

    @Test
    void listarUsuarios_devuelveDtosDeTodosLosUsuarios() {
        when(usuarioDao.findAll()).thenReturn(List.of(new Usuario(1L, "Ana", "ana@example.com")));

        assertEquals("Ana", usuarioService.listarUsuarios().get(0).nombre());
    }

    @Test
    void crearUsuario_validaNormalizaYDevuelveDto() {
        when(usuarioDao.findByEmail("ana@example.com")).thenReturn(Optional.empty());
        when(usuarioDao.insert(any(Usuario.class))).thenReturn(new Usuario(2L, "Ana", "ana@example.com"));

        var creado = usuarioService.crearUsuario(new GuardarUsuarioDto(" Ana ", "ANA@example.com "));

        assertEquals(2L, creado.id());
        assertEquals("ana@example.com", creado.email());
        verify(usuarioDao).insert(new Usuario(null, "Ana", "ana@example.com"));
    }

    @Test
    void crearUsuario_emailDuplicado_lanzaDuplicateException() {
        when(usuarioDao.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(new Usuario(1L, "Ana", "ana@example.com")));

        assertThrows(DuplicateException.class,
                () -> usuarioService.crearUsuario(new GuardarUsuarioDto("Otra", "ana@example.com")));
    }

    @Test
    void crearUsuario_emailInvalido_lanzaValidationException() {
        assertThrows(ValidationException.class,
                () -> usuarioService.crearUsuario(new GuardarUsuarioDto("Ana", "correo-invalido")));
    }
}