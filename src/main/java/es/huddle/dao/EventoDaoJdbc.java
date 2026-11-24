package es.huddle.dao;

import es.huddle.model.Evento;
import es.huddle.model.TipoEvento;
import es.huddle.util.TransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EventoDaoJdbc extends JdbcDaoSupport implements EventoDao {
    public EventoDaoJdbc(TransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public Optional<Evento> findById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, equipo_id, tipo, titulo, fecha_hora, lugar, rival FROM eventos WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Evento> findByEquipoId(long equipoId) {
        return findMany("SELECT id, equipo_id, tipo, titulo, fecha_hora, lugar, rival FROM eventos " +
                "WHERE equipo_id = ? ORDER BY fecha_hora, id", equipoId);
    }

    @Override
    public List<Evento> findAll() {
        return execute(connection -> {
            List<Evento> eventos = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, equipo_id, tipo, titulo, fecha_hora, lugar, rival FROM eventos ORDER BY fecha_hora, id");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    eventos.add(map(resultSet));
                }
            }
            return eventos;
        });
    }

    @Override
    public Evento insert(Evento evento) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO eventos (equipo_id, tipo, titulo, fecha_hora, lugar, rival) VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                bind(statement, evento);
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("No se generó el identificador del evento.");
                    }
                    return new Evento(generatedKeys.getLong(1), evento.equipoId(), evento.tipo(), evento.titulo(),
                            evento.fechaHora(), evento.lugar(), evento.rival());
                }
            }
        });
    }

    @Override
    public int update(Evento evento) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE eventos SET equipo_id = ?, tipo = ?, titulo = ?, fecha_hora = ?, lugar = ?, rival = ? " +
                            "WHERE id = ?")) {
                bind(statement, evento);
                statement.setLong(7, evento.id());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int deleteById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM eventos WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate();
            }
        });
    }

    private List<Evento> findMany(String sql, long equipoId) {
        return execute(connection -> {
            List<Evento> eventos = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, equipoId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        eventos.add(map(resultSet));
                    }
                }
            }
            return eventos;
        });
    }

    private void bind(PreparedStatement statement, Evento evento) throws SQLException {
        statement.setLong(1, evento.equipoId());
        statement.setString(2, evento.tipo().name());
        statement.setString(3, evento.titulo());
        statement.setTimestamp(4, Timestamp.valueOf(evento.fechaHora()));
        statement.setString(5, evento.lugar());
        statement.setString(6, evento.rival());
    }

    private Evento map(ResultSet resultSet) throws SQLException {
        Timestamp fechaHora = resultSet.getTimestamp("fecha_hora");
        return new Evento(resultSet.getLong("id"), resultSet.getLong("equipo_id"),
                TipoEvento.valueOf(resultSet.getString("tipo")), resultSet.getString("titulo"),
                fechaHora.toLocalDateTime(), resultSet.getString("lugar"), resultSet.getString("rival"));
    }
}