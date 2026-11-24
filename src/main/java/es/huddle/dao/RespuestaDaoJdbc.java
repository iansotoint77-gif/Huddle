package es.huddle.dao;

import es.huddle.model.EstadoAsistencia;
import es.huddle.model.Respuesta;
import es.huddle.util.TransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RespuestaDaoJdbc extends JdbcDaoSupport implements RespuestaDao {
    public RespuestaDaoJdbc(TransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public Optional<Respuesta> findById(long eventoId, long usuarioId) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT evento_id, usuario_id, estado, comentario, fecha_respuesta FROM respuestas " +
                            "WHERE evento_id = ? AND usuario_id = ?")) {
                statement.setLong(1, eventoId);
                statement.setLong(2, usuarioId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Respuesta> findByEventoId(long eventoId) {
        return findMany("SELECT evento_id, usuario_id, estado, comentario, fecha_respuesta FROM respuestas " +
                "WHERE evento_id = ? ORDER BY usuario_id", eventoId);
    }

    @Override
    public List<Respuesta> findByUsuarioId(long usuarioId) {
        return findMany("SELECT evento_id, usuario_id, estado, comentario, fecha_respuesta FROM respuestas " +
                "WHERE usuario_id = ? ORDER BY evento_id", usuarioId);
    }

    @Override
    public int upsert(Respuesta respuesta) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO respuestas (evento_id, usuario_id, estado, comentario, fecha_respuesta) " +
                            "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE estado = VALUES(estado), " +
                            "comentario = VALUES(comentario), fecha_respuesta = VALUES(fecha_respuesta)")) {
                statement.setLong(1, respuesta.eventoId());
                statement.setLong(2, respuesta.usuarioId());
                statement.setString(3, respuesta.estado().name());
                if (respuesta.comentario() == null) {
                    statement.setNull(4, Types.VARCHAR);
                } else {
                    statement.setString(4, respuesta.comentario());
                }
                statement.setTimestamp(5, Timestamp.valueOf(respuesta.fechaRespuesta()));
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int deleteById(long eventoId, long usuarioId) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM respuestas WHERE evento_id = ? AND usuario_id = ?")) {
                statement.setLong(1, eventoId);
                statement.setLong(2, usuarioId);
                return statement.executeUpdate();
            }
        });
    }

    private List<Respuesta> findMany(String sql, long parameter) {
        return execute(connection -> {
            List<Respuesta> respuestas = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, parameter);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        respuestas.add(map(resultSet));
                    }
                }
            }
            return respuestas;
        });
    }

    private Respuesta map(ResultSet resultSet) throws SQLException {
        Timestamp fechaRespuesta = resultSet.getTimestamp("fecha_respuesta");
        return new Respuesta(resultSet.getLong("evento_id"), resultSet.getLong("usuario_id"),
                EstadoAsistencia.valueOf(resultSet.getString("estado")), resultSet.getString("comentario"),
                fechaRespuesta.toLocalDateTime());
    }
}