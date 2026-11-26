package es.huddle.util;

import es.huddle.dto.GuardarEventoDto;
import es.huddle.exception.ValidationException;
import es.huddle.model.TipoEvento;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonUtilTest {
    @Test
    void read_aceptaFechaIso8601EnDto() throws Exception {
        HttpServletRequest request = requestWithBody("""
                {"tipo":"ENTRENAMIENTO","titulo":"Sesión","fechaHora":"2026-12-01T10:00:00","lugar":"Pabellón"}
                """);

        GuardarEventoDto dto = JsonUtil.read(request, GuardarEventoDto.class);

        assertEquals(TipoEvento.ENTRENAMIENTO, dto.tipo());
        assertEquals(LocalDateTime.of(2026, 12, 1, 10, 0), dto.fechaHora());
    }

    @Test
    void read_aceptaPartidoDelFormularioConFechaHoraIso() throws Exception {
        HttpServletRequest request = requestWithBody("""
                {"tipo":"PARTIDO","titulo":"LICEO FRANCES VS ZENTRO BASKET","fechaHora":"2026-11-29T16:00:00","lugar":"Liceo Arena","rival":"Zentro Basket"}
                """);

        GuardarEventoDto dto = JsonUtil.read(request, GuardarEventoDto.class);

        assertEquals(TipoEvento.PARTIDO, dto.tipo());
        assertEquals("LICEO FRANCES VS ZENTRO BASKET", dto.titulo());
        assertEquals(LocalDateTime.of(2026, 11, 29, 16, 0), dto.fechaHora());
        assertEquals("Liceo Arena", dto.lugar());
        assertEquals("Zentro Basket", dto.rival());
    }

    @Test
    void read_rechazaJsonNoEstandar() throws Exception {
        HttpServletRequest request = requestWithBody("{tipo:ENTRENAMIENTO}");

        assertThrows(ValidationException.class, () -> JsonUtil.read(request, GuardarEventoDto.class));
    }

    private HttpServletRequest requestWithBody(String body) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getReader()).thenReturn(new BufferedReader(new StringReader(body)));
        return request;
    }
}