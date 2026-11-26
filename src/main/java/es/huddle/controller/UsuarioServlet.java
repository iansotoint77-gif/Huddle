package es.huddle.controller;

import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.dto.ActualizarNombreUsuarioDto;
import es.huddle.exception.ValidationException;
import es.huddle.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/usuarios/*")
public final class UsuarioServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        writeJson(response, email == null ? usuarioService().listarUsuarios() : usuarioService().buscarPorEmail(email));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var usuario = usuarioService().crearUsuario(JsonUtil.read(request, GuardarUsuarioDto.class));
        created(response, request.getContextPath() + "/api/usuarios/" + usuario.id(), usuario);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long usuarioId = pathUsuarioId(request.getPathInfo());
        var actualizado = usuarioService().actualizarNombre(usuarioId(request), usuarioId,
                JsonUtil.read(request, ActualizarNombreUsuarioDto.class));
        writeJson(response, actualizado);
    }

    private long pathUsuarioId(String pathInfo) {
        if (pathInfo == null || !pathInfo.matches("/\\d+")) {
            throw new ValidationException("La ruta debe incluir un id de usuario válido.");
        }
        try {
            long id = Long.parseLong(pathInfo.substring(1));
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
            // El id fuera de rango se trata como una ruta no válida.
        }
        throw new ValidationException("La ruta debe incluir un id de usuario válido.");
    }
}