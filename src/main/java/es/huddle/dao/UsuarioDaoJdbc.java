package es.huddle.dao;

import es.huddle.model.Usuario;
import es.huddle.util.TransactionManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UsuarioDaoJdbc extends JdbcDaoSupport implements UsuarioDao {
    public UsuarioDaoJdbc(TransactionManager transactionManager) {
        super(transactionManager);
    }

    @Override
    public Optional<Usuario> findById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, nombre, email FROM usuarios WHERE id = ?")) {
                statement.setLong(1, id);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, nombre, email FROM usuarios WHERE email = ?")) {
                statement.setString(1, email);
                try (ResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
                }
            }
        });
    }

    @Override
    public List<Usuario> findAll() {
        return execute(connection -> {
            List<Usuario> usuarios = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id, nombre, email FROM usuarios ORDER BY id");
                 ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    usuarios.add(map(resultSet));
                }
            }
            return usuarios;
        });
    }

    @Override
    public Usuario insert(Usuario usuario) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO usuarios (nombre, email) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, usuario.nombre());
                statement.setString(2, usuario.email());
                statement.executeUpdate();
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        throw new SQLException("No se generó el identificador del usuario.");
                    }
                    return new Usuario(generatedKeys.getLong(1), usuario.nombre(), usuario.email());
                }
            }
        });
    }

    @Override
    public int update(Usuario usuario) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE usuarios SET nombre = ?, email = ? WHERE id = ?")) {
                statement.setString(1, usuario.nombre());
                statement.setString(2, usuario.email());
                statement.setLong(3, usuario.id());
                return statement.executeUpdate();
            }
        });
    }

    @Override
    public int deleteById(long id) {
        return execute(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM usuarios WHERE id = ?")) {
                statement.setLong(1, id);
                return statement.executeUpdate();
            }
        });
    }

    private Usuario map(ResultSet resultSet) throws SQLException {
        return new Usuario(resultSet.getLong("id"), resultSet.getString("nombre"), resultSet.getString("email"));
    }
}