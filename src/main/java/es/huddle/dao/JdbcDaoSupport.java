package es.huddle.dao;

import es.huddle.exception.DataAccessException;
import es.huddle.exception.DuplicateException;
import es.huddle.util.TransactionManager;

import java.sql.SQLException;

abstract class JdbcDaoSupport {
    private final TransactionManager transactionManager;

    protected JdbcDaoSupport(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    protected <T> T execute(TransactionManager.SqlFunction<T> operation) {
        try {
            return transactionManager.execute(operation);
        } catch (DataAccessException exception) {
            SQLException sqlException = findSqlException(exception);
            if (sqlException != null && sqlException.getErrorCode() == 1062) {
                throw new DuplicateException("El registro ya existe.", exception);
            }
            throw exception;
        }
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return sqlException;
            }
            current = current.getCause();
        }
        return null;
    }
}