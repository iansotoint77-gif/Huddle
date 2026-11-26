package es.huddle.controller;

import es.huddle.dto.ActualizarDorsalDto;
import es.huddle.dto.AnadirMiembroDto;
import es.huddle.dto.GuardarEquipoDto;
import es.huddle.dto.GuardarEventoDto;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;

@WebServlet(urlPatterns = {"/api/equipos", "/api/equipos/*"})
public final class EquipoServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 0) {
            writeJson(response, equipoService().listarEquipos(usuarioId(request)));
        } else if (path.length == 1) {
            writeJson(response, equipoService().obtenerEquipo(usuarioId(request), id(path[0])));
        } else if (path.length == 2 && "miembros".equals(path[1])) {
            writeJson(response, equipoService().listarMiembros(usuarioId(request), id(path[0])));
        } else if (path.length == 2 && "eventos".equals(path[1])) {
            writeJson(response, equipoService().listarEventos(usuarioId(request), id(path[0]),
                    request.getParameter("tiempo")));
        } else {
            throw new NotFoundException("No existe la ruta solicitada.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 0) {
            var equipo = equipoService().crearEquipo(usuarioId(request), JsonUtil.read(request, GuardarEquipoDto.class));
            created(response, request.getContextPath() + "/api/equipos/" + equipo.id(), equipo);
        } else if (path.length == 2 && "miembros".equals(path[1])) {
            var miembro = equipoService().agregarJugador(usuarioId(request), id(path[0]),
                    JsonUtil.read(request, AnadirMiembroDto.class));
            created(response, request.getContextPath() + "/api/equipos/" + path[0] + "/miembros/" +
                    miembro.usuarioId(), miembro);
        } else if (path.length == 2 && "eventos".equals(path[1])) {
            var evento = eventoService().crearEvento(usuarioId(request), id(path[0]),
                    JsonUtil.read(request, GuardarEventoDto.class));
            created(response, request.getContextPath() + "/api/eventos/" + evento.id(), evento);
        } else {
            throw new NotFoundException("No existe la ruta solicitada.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 1) {
            writeJson(response, equipoService().actualizarEquipo(usuarioId(request), id(path[0]),
                    JsonUtil.read(request, GuardarEquipoDto.class)));
            return;
        }
        throw new NotFoundException("No existe la ruta solicitada.");
    }

    @Override
    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 3 && "miembros".equals(path[1])) {
            writeJson(response, equipoService().cambiarDorsal(usuarioId(request), id(path[0]), id(path[2]),
                    JsonUtil.read(request, ActualizarDorsalDto.class)));
            return;
        }
        throw new NotFoundException("No existe la ruta solicitada.");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 1) {
            equipoService().eliminarEquipo(usuarioId(request), id(path[0]));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        } else if (path.length == 3 && "miembros".equals(path[1])) {
            equipoService().eliminarMiembro(usuarioId(request), id(path[0]), id(path[2]));
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        throw new NotFoundException("No existe la ruta solicitada.");
    }

    private String[] path(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty() || "/".equals(pathInfo)) {
            return new String[0];
        }
        String[] segments = pathInfo.substring(1).split("/", -1);
        if (Arrays.stream(segments).anyMatch(String::isEmpty)) {
            throw new ValidationException("La ruta contiene un segmento vacío.");
        }
        return segments;
    }

    private long id(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new ValidationException("Los identificadores deben ser números enteros positivos.");
    }
}