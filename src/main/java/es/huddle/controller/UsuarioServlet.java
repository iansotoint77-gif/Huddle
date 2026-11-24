package es.huddle.controller;

import es.huddle.dto.GuardarUsuarioDto;
import es.huddle.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/usuarios")
public final class UsuarioServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeJson(response, usuarioService().listarUsuarios());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var usuario = usuarioService().crearUsuario(JsonUtil.read(request, GuardarUsuarioDto.class));
        created(response, request.getContextPath() + "/api/usuarios/" + usuario.id(), usuario);
    }
}