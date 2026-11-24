package es.huddle.dao;

import es.huddle.model.Respuesta;

import java.util.List;
import java.util.Optional;

public interface RespuestaDao {
    Optional<Respuesta> findById(long eventoId, long usuarioId);

    List<Respuesta> findByEventoId(long eventoId);

    List<Respuesta> findByUsuarioId(long usuarioId);

    int upsert(Respuesta respuesta);

    int deleteById(long eventoId, long usuarioId);
}