package es.huddle.controller;

import es.huddle.config.AppConfig;
import es.huddle.exception.DataAccessException;
import es.huddle.exception.DuplicateException;
import es.huddle.exception.ForbiddenException;
import es.huddle.exception.NotFoundException;
import es.huddle.exception.ValidationException;
import es.huddle.filter.UsuarioFilter;
import es.huddle.service.EquipoService;
import es.huddle.service.EventoService;
import es.huddle.service.RespuestaService;
import es.huddle.service.UsuarioService;
import es.huddle.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class BaseServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(BaseServlet.class.getName());

    @Override
    protected final void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            if ("PATCH".equalsIgnoreCase(request.getMethod())) {
                doPatch(request, response);
            } else {
                super.service(request, response);
            }
        } catch (RuntimeException exception) {
            handleException(response, exception);
        }
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        methodNotAllowed(response);
    }

    protected final long usuarioId(HttpServletRequest request) {
        Object value = request.getAttribute(UsuarioFilter.ATTRIBUTE);
        if (value instanceof Long usuarioId) {
            return usuarioId;
        }
        throw new ValidationException("No se ha identificado el usuario actual.");
    }

    protected final UsuarioService usuarioService() {
        return (UsuarioService) getServletContext().getAttribute(AppConfig.USUARIO_SERVICE);
    }

    protected final EquipoService equipoService() {
        return (EquipoService) getServletContext().getAttribute(AppConfig.EQUIPO_SERVICE);
    }

    protected final EventoService eventoService() {
        return (EventoService) getServletContext().getAttribute(AppConfig.EVENTO_SERVICE);
    }

    protected final RespuestaService respuestaService() {
        return (RespuestaService) getServletContext().getAttribute(AppConfig.RESPUESTA_SERVICE);
    }

    protected final void writeJson(HttpServletResponse response, Object value) throws IOException {
        JsonUtil.write(response, HttpServletResponse.SC_OK, value);
    }

    protected final void created(HttpServletResponse response, String location, Object value) throws IOException {
        response.setHeader("Location", location);
        JsonUtil.write(response, HttpServletResponse.SC_CREATED, value);
    }

    private void methodNotAllowed(HttpServletResponse response) throws IOException {
        JsonUtil.writeError(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed",
                "El método HTTP no está permitido para este recurso.");
    }

    private void handleException(HttpServletResponse response, RuntimeException exception) throws IOException {
        if (response.isCommitted()) {
            LOGGER.log(Level.SEVERE, "Error después de iniciar la respuesta HTTP.", exception);
            return;
        }
        response.reset();
        int status;
        String error;
        String message;
        if (exception instanceof ValidationException) {
            status = HttpServletResponse.SC_BAD_REQUEST;
            error = "Bad Request";
            message = exception.getMessage();
        } else if (exception instanceof ForbiddenException) {
            status = HttpServletResponse.SC_FORBIDDEN;
            error = "Forbidden";
            message = exception.getMessage();
        } else if (exception instanceof NotFoundException) {
            status = HttpServletResponse.SC_NOT_FOUND;
            error = "Not Found";
            message = exception.getMessage();
        } else if (exception instanceof DuplicateException) {
            status = HttpServletResponse.SC_CONFLICT;
            error = "Conflict";
            message = exception.getMessage();
        } else {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            error = "Internal Server Error";
            message = "Se ha producido un error interno del servidor.";
            LOGGER.log(Level.SEVERE, exception instanceof DataAccessException ?
                    "Error de acceso a datos en la API." : "Error no controlado en la API.", exception);
        }
        JsonUtil.writeError(response, status, error, message == null ? error : message);
    }
}