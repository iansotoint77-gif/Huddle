package es.huddle.service;

import es.huddle.dao.UsuarioDao;
import es.huddle.dto.ActualizarNombreUsuarioDto;
import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
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

    @Test
    void buscarPorEmail_usuarioExistente_devuelveDtoNormalizado() {
        when(usuarioDao.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(new Usuario(1L, "Ana", "ana@example.com")));

        assertEquals(1L, usuarioService.buscarPorEmail(" ANA@example.com ").id());
    }

    @Test
    void buscarPorEmail_usuarioInexistente_lanzaNotFoundExceptionConIndicacionDeRegistro() {
        when(usuarioDao.findByEmail("ana@example.com")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> usuarioService.buscarPorEmail("ana@example.com"));

        org.junit.jupiter.api.Assertions.assertTrue(exception.getMessage().contains("Regístrate"));
    }

    @Test
    void actualizarNombre_usuarioPropio_actualizaElPerfilSinCambiarEmail() {
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Ana", "ana@example.com")));
        when(usuarioDao.update(new Usuario(1L, "Ana Ruiz", "ana@example.com"))).thenReturn(1);

        var actualizado = usuarioService.actualizarNombre(1L, 1L, new ActualizarNombreUsuarioDto(" Ana Ruiz "));

        assertEquals("Ana Ruiz", actualizado.nombre());
        assertEquals("ana@example.com", actualizado.email());
        verify(usuarioDao).update(new Usuario(1L, "Ana Ruiz", "ana@example.com"));
    }

    @Test
    void actualizarNombre_usuarioInexistente_lanzaNotFoundAntesDeComprobarPermiso() {
        when(usuarioDao.findById(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> usuarioService.actualizarNombre(1L, 2L, new ActualizarNombreUsuarioDto("Ana")));
    }

    @Test
    void actualizarNombre_perfilDeOtroUsuario_lanzaForbiddenException() {
        when(usuarioDao.findById(2L)).thenReturn(Optional.of(new Usuario(2L, "Beto", "beto@example.com")));

        assertThrows(ForbiddenException.class,
                () -> usuarioService.actualizarNombre(1L, 2L, new ActualizarNombreUsuarioDto("Beto")));
    }

    @Test
    void actualizarNombre_nombreVacio_lanzaValidationException() {
        when(usuarioDao.findById(1L)).thenReturn(Optional.of(new Usuario(1L, "Ana", "ana@example.com")));

        assertThrows(ValidationException.class,
                () -> usuarioService.actualizarNombre(1L, 1L, new ActualizarNombreUsuarioDto(" ")));
    }
}