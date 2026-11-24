package es.huddle.controller;

import com.google.gson.Gson;
import es.huddle.config.DataSourceProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(HealthServlet.class.getName());
    private static final Gson GSON = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            DataSource dataSource = DataSourceProvider.getDataSource();
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT 1");
                 ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new SQLException("La consulta de salud no devolvió el resultado esperado.");
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(GSON.toJson(Map.of("status", "UP")));
        } catch (SQLException | IllegalStateException exception) {
            LOGGER.log(Level.SEVERE, "No se pudo comprobar la conexión con la base de datos.", exception);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write(GSON.toJson(Map.of("status", "DOWN", "error", "Database unavailable")));
        }
    }
}