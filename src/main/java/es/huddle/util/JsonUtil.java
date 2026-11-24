package es.huddle.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.Strictness;
import es.huddle.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

public final class JsonUtil {
    private static final Gson GSON = new GsonBuilder()
            .setStrictness(Strictness.STRICT)
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (value, type, context) -> new JsonPrimitive(value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>)
                    (json, type, context) -> {
                        try {
                            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        } catch (DateTimeParseException | IllegalStateException exception) {
                            throw new JsonParseException("Fecha y hora no válidas.", exception);
                        }
                    })
            .create();

    private JsonUtil() {
    }

    public static <T> T read(HttpServletRequest request, Class<T> type) throws IOException {
        try {
            return GSON.fromJson(request.getReader(), type);
        } catch (JsonParseException exception) {
            throw new ValidationException("El cuerpo JSON no es válido.");
        }
    }

    public static void write(HttpServletResponse response, int status, Object value) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(GSON.toJson(value));
    }

    public static void writeError(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        write(response, status, Map.of("status", status, "error", error, "message", message));
    }
}