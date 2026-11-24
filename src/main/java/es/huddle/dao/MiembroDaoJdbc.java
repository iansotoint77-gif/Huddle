package es.huddle.dao;

import es.huddle.model.MiembroEquipo;
import es.huddle.model.Rol;
import es.huddle.util.TransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MiembroDaoJdbc extends JdbcDaoSupport implements MiembroDao {
    public MiembroDaoJdbc(TransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public Optional<MiembroEquipo> findById(long equipoId, long usuarioId) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT equipo_id, usuario_id, rol, dorsal FROM miembros_equipo " +
                            "WHERE equipo_id = ? AND usuario_id = ?")) {
                statement.setLong(1, equipoId);
                statement.setLong(2, usuarioId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<MiembroEquipo> findByEquipoId(long equipoId) {
        return findMany("SELECT equipo_id, usuario_id, rol, dorsal FROM miembros_equipo " +
                "WHERE equipo_id = ? ORDER BY usuario_id", equipoId);
    }

    @Override
    public List<MiembroEquipo> findByUsuarioId(long usuarioId) {
        return findMany("SELECT equipo_id, usuario_id, rol, dorsal FROM miembros_equipo " +
                "WHERE usuario_id = ? ORDER BY equipo_id", usuarioId);
    }

    @Override
    public boolean existsById(long equipoId, long usuarioId) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT 1 FROM miembros_equipo WHERE equipo_id = ? AND usuario_id = ?")) {
                statement.setLong(1, equipoId);
                statement.setLong(2, usuarioId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next();
                }
            }
        });
    }

    @Override
    public int insert(MiembroEquipo miembro) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO miembros_equipo (equipo_id, usuario_id, rol, dorsal) VALUES (?, ?, ?, ?)")) {
                statement.setLong(1, miembro.equipoId());
                statement.setLong(2, miembro.usuarioId());
                statement.setString(3, miembro.rol().name());
                setDorsal(statement, 4, miembro.dorsal());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int updateDorsal(long equipoId, long usuarioId, Integer dorsal) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE miembros_equipo SET dorsal = ? WHERE equipo_id = ? AND usuario_id = ?")) {
                setDorsal(statement, 1, dorsal);
                statement.setLong(2, equipoId);
                statement.setLong(3, usuarioId);
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int deleteById(long equipoId, long usuarioId) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "DELETE FROM miembros_equipo WHERE equipo_id = ? AND usuario_id = ?")) {
                statement.setLong(1, equipoId);
                statement.setLong(2, usuarioId);
                return statement.executeUpdate();
            }
        });
    }

    private List<MiembroEquipo> findMany(String sql, long parameter) {
        return execute(connection -> {
            List<MiembroEquipo> miembros = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, parameter);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        miembros.add(map(resultSet));
                    }
                }
            }
            return miembros;
        });
    }

    private MiembroEquipo map(ResultSet resultSet) throws SQLException {
        int dorsalValue = resultSet.getInt("dorsal");
        Integer dorsal = resultSet.wasNull() ? null : dorsalValue;
        return new MiembroEquipo(resultSet.getLong("equipo_id"), resultSet.getLong("usuario_id"),
                Rol.valueOf(resultSet.getString("rol")), dorsal);
    }

    private void setDorsal(PreparedStatement statement, int index, Integer dorsal) throws SQLException {
        if (dorsal == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, dorsal);
        }
    }
}