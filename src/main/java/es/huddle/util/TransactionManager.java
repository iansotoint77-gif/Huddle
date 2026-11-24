package es.huddle.util;

import es.huddle.exception.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

public final class TransactionManager {
    private final DataSource dataSource;
    private final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

    public TransactionManager(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public void begin() {
        if (transactionConnection.get() != null) {
            throw new IllegalStateException("Ya hay una transacción activa en este hilo.");
        }

        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            transactionConnection.set(connection);
        } catch (SQLException exception) {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException closeException) {
                    exception.addSuppressed(closeException);
                }
            }
            throw new DataAccessException("No se pudo iniciar la transacción.", exception);
        }
    }

    public void commit() {
        finish(true);
    }

    public void rollback() {
        finish(false);
    }

    public boolean isTransactionActive() {
        return transactionConnection.get() != null;
    }

    public <T> T execute(SqlFunction<T> operation) {
        Connection current = transactionConnection.get();
        if (current != null) {
            try {
                return operation.apply(current);
            } catch (SQLException exception) {
                throw new DataAccessException("Falló una operación de base de datos.", exception);
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            return operation.apply(connection);
        } catch (SQLException exception) {
            throw new DataAccessException("Falló una operación de base de datos.", exception);
        }
    }

    private void finish(boolean commit) {
        Connection connection = transactionConnection.get();
        if (connection == null) {
            throw new IllegalStateException("No hay una transacción activa en este hilo.");
        }

        SQLException failure = null;
        try {
            if (commit) {
                connection.commit();
            } else {
                connection.rollback();
            }
        } catch (SQLException exception) {
            failure = exception;
            if (commit) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    failure.addSuppressed(rollbackException);
                }
            }
        } finally {
            transactionConnection.remove();
            try {
                connection.close();
            } catch (SQLException closeException) {
                if (failure == null) {
                    failure = closeException;
                } else {
                    failure.addSuppressed(closeException);
                }
            }
        }

        if (failure != null) {
            throw new DataAccessException(commit ? "No se pudo confirmar la transacción." :
                    "No se pudo revertir la transacción.", failure);
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {
        T apply(Connection connection) throws SQLException;
    }
}