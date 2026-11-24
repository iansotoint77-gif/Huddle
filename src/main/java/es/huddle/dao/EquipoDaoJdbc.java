package es.huddle.dao;

import es.huddle.model.Equipo;
import es.huddle.util.TransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EquipoDaoJdbc extends JdbcDaoSupport implements EquipoDao {
    public EquipoDaoJdbc(TransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public Optional<Equipo> findById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, nombre, categoria, creador_id FROM equipos WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Equipo> findByCreadorId(long creadorId) {
        return findMany("SELECT id, nombre, categoria, creador_id FROM equipos WHERE creador_id = ? ORDER BY id",
                creadorId);
    }

    @Override
    public List<Equipo> findByUsuarioId(long usuarioId) {
        return execute(connection -> {
            List<Equipo> equipos = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT e.id, e.nombre, e.categoria, e.creador_id " +
                            "FROM equipos e JOIN miembros_equipo m ON m.equipo_id = e.id " +
                            "WHERE m.usuario_id = ? ORDER BY e.id")) {
                statement.setLong(1, usuarioId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        equipos.add(map(resultSet));
                    }
                }
            }
            return equipos;
        });
    }

    @Override
    public List<Equipo> findAll() {
        return execute(connection -> {
            List<Equipo> equipos = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, nombre, categoria, creador_id FROM equipos ORDER BY id");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    equipos.add(map(resultSet));
                }
            }
            return equipos;
        });
    }

    @Override
    public Equipo insert(Equipo equipo) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO equipos (nombre, categoria, creador_id) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, equipo.nombre());
                statement.setString(2, equipo.categoria());
                statement.setLong(3, equipo.creadorId());
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("No se generó el identificador del equipo.");
                    }
                    return new Equipo(generatedKeys.getLong(1), equipo.nombre(), equipo.categoria(), equipo.creadorId());
                }
            }
        });
    }

    @Override
    public int update(Equipo equipo) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE equipos SET nombre = ?, categoria = ?, creador_id = ? WHERE id = ?")) {
                statement.setString(1, equipo.nombre());
                statement.setString(2, equipo.categoria());
                statement.setLong(3, equipo.creadorId());
                statement.setLong(4, equipo.id());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int deleteById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM equipos WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate();
            }
        });
    }

    private List<Equipo> findMany(String sql, long parameter) {
        return execute(connection -> {
            List<Equipo> equipos = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, parameter);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        equipos.add(map(resultSet));
                    }
                }
            }
            return equipos;
        });
    }

    private Equipo map(ResultSet resultSet) throws SQLException {
        return new Equipo(resultSet.getLong("id"), resultSet.getString("nombre"), resultSet.getString("categoria"),
                resultSet.getLong("creador_id"));
    }
}