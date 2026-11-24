package es.huddle.filter;

import es.huddle.util.JsonUtil;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebFilter(urlPatterns = "/api/*", dispatcherTypes = DispatcherType.REQUEST)
public final class UsuarioFilter implements Filter {
    public static final String HEADER = "X-Usuario-Id";
    public static final String ATTRIBUTE = "es.huddle.usuarioId";

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/api/usuarios".equals(path) || "/api/health".equals(path)) {
            chain.doFilter(request, response);
            return;
        }

        String rawId = request.getHeader(HEADER);
        long usuarioId;
        try {
            usuarioId = Long.parseLong(rawId);
            if (usuarioId < 1) {
                throw new NumberFormatException("El id debe ser positivo.");
            }
        } catch (NumberFormatException | NullPointerException exception) {
            JsonUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, "Bad Request",
                    "La cabecera X-Usuario-Id es obligatoria y debe contener un id positivo.");
            return;
        }

        request.setAttribute(ATTRIBUTE, usuarioId);
        chain.doFilter(request, response);
    }
}