package es.huddle.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DataSourceProvider {
    private static final String DEFAULT_POOL_SIZE = "10";

    private DataSourceProvider() {
    }

    public static DataSource getDataSource() {
        return Holder.DATA_SOURCE;
    }

    private static HikariDataSource createDataSource() {
        String host = requiredEnvironmentVariable("DB_HOST");
        String portValue = requiredEnvironmentVariable("DB_PORT");
        String database = requiredEnvironmentVariable("DB_NAME");
        String user = requiredEnvironmentVariable("DB_USER");
        String password = requiredEnvironmentVariable("DB_PASSWORD");
        String poolSizeValue = System.getenv().getOrDefault("DB_POOL_SIZE", DEFAULT_POOL_SIZE);

        int port;
        try {
            port = Integer.parseInt(portValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("La variable DB_PORT debe ser un puerto TCP entre 1 y 65535.", exception);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("La variable DB_PORT debe ser un puerto TCP entre 1 y 65535.");
        }

        int poolSize;
        try {
            poolSize = Integer.parseInt(poolSizeValue);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("La variable DB_POOL_SIZE debe ser un número entero positivo.", exception);
        }
        if (poolSize < 1) {
            throw new IllegalStateException("La variable DB_POOL_SIZE debe ser un número entero positivo.");
        }

        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        config.addDataSourceProperty("serverName", host);
        config.addDataSourceProperty("port", port);
        config.addDataSourceProperty("databaseName", database);
        config.addDataSourceProperty("user", user);
        config.addDataSourceProperty("password", password);
        config.setMaximumPoolSize(poolSize);
        config.setPoolName("huddle-db-pool");
        return new HikariDataSource(config);
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno obligatoria " + name + ".");
        }
        return value;
    }

    private static class Holder {
        private static final HikariDataSource DATA_SOURCE = createDataSource();
    }
}