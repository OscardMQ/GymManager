package com.gymmanager.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Conexión a la base de datos SQLite usando el patrón Singleton.
 *
 * La BD se almacena en ~/.gymmanager/gymmanager.db para no contaminar
 * el directorio de trabajo y persistir entre ejecuciones.
 *
 * Uso: DatabaseConnection.getInstance().getConnection()
 */
public class DatabaseConnection {

    private static DatabaseConnection instancia;
    private Connection conexion;

    // Carpeta de datos en el directorio home del usuario del SO
    private static final Path DIR_DATOS = Path.of(System.getProperty("user.home"), ".gymmanager");
    private static final String URL_JDBC = "jdbc:sqlite:" + DIR_DATOS.resolve("gymmanager.db");

    /** Constructor privado: evita instanciación externa */
    private DatabaseConnection() throws SQLException {
        asegurarDirectorio();
        conexion = DriverManager.getConnection(URL_JDBC);
        aplicarPragmas();
    }

    /**
     * Retorna la instancia única. Si la conexión fue cerrada, la recrea.
     * Sincronizado para soporte básico de múltiples hilos.
     */
    public static synchronized DatabaseConnection getInstance() throws SQLException {
        if (instancia == null || instancia.conexion.isClosed()) {
            instancia = new DatabaseConnection();
        }
        return instancia;
    }

    /** Retorna la conexión SQL activa */
    public Connection getConnection() {
        return conexion;
    }

    /** Cierra la conexión de forma segura al apagar la app */
    public void cerrar() throws SQLException {
        if (conexion != null && !conexion.isClosed()) {
            conexion.close();
        }
    }

    /** Activa foreign keys y modo WAL para mejor rendimiento y consistencia */
    private void aplicarPragmas() throws SQLException {
        try (Statement stmt = conexion.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            // WAL permite lecturas concurrentes mientras hay escrituras
            stmt.execute("PRAGMA journal_mode = WAL");
        }
    }

    /** Crea el directorio ~/.gymmanager si no existe todavía */
    private void asegurarDirectorio() throws SQLException {
        try {
            Files.createDirectories(DIR_DATOS);
        } catch (IOException e) {
            throw new SQLException("No se pudo crear el directorio de datos: " + DIR_DATOS, e);
        }
    }
}
