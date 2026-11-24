package es.huddle.controller;

import es.huddle.dto.GuardarEventoDto;
import es.huddle.dto.GuardarRespuestaDto;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/eventos/*")
public final class EventoServlet extends BaseServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 1) {
            writeJson(response, eventoService().obtenerEvento(usuarioId(request), id(path[0])));
        } else if (path.length == 2 && "respuestas".equals(path[1])) {
            writeJson(response, respuestaService().listarRespuestas(usuarioId(request), id(path[0])));
        } else {
            throw new NotFoundException("No existe la ruta solicitada.");
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 1) {
            writeJson(response, eventoService().actualizarEvento(usuarioId(request), id(path[0]),
                    JsonUtil.read(request, GuardarEventoDto.class)));
        } else if (path.length == 2 && "respuesta".equals(path[1])) {
            writeJson(response, respuestaService().guardarRespuesta(usuarioId(request), id(path[0]),
                    JsonUtil.read(request, GuardarRespuestaDto.class)));
        } else {
            throw new NotFoundException("No existe la ruta solicitada.");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] path = path(request);
        if (path.length == 1) {
            eventoService().eliminarEvento(usuarioId(request), id(path[0]));
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
        for (String segment : segments) {
            if (segment.isEmpty()) {
                throw new ValidationException("La ruta contiene un segmento vacío.");
            }
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