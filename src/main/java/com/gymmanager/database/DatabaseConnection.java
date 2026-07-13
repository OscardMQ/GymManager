package com.gymmanager.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Fábrica de conexiones a la base de datos SQLite.
 *
 * Cada llamada a getConnection() abre una conexión NUEVA que el llamador
 * debe cerrar (try-with-resources). Esto elimina la conexión compartida
 * entre hilos: con WAL activo SQLite soporta lecturas concurrentes, y
 * busy_timeout hace que un escritor espere al otro en vez de fallar
 * con "database is locked".
 *
 * Uso: try (Connection conn = DatabaseConnection.getConnection()) { ... }
 */
public class DatabaseConnection {

    // Carpeta de datos en el home del usuario.
    // -Dgymmanager.dir=... permite apuntar a otra carpeta (pruebas).
    private static final Path DIR_DATOS = Path.of(
            System.getProperty("gymmanager.dir",
                    Path.of(System.getProperty("user.home"), ".gymmanager").toString()));

    private static final String URL_JDBC = "jdbc:sqlite:" + DIR_DATOS.resolve("gymmanager.db");

    private static volatile boolean directorioListo = false;

    /** Abre una conexión nueva con los PRAGMA aplicados. El llamador la cierra. */
    public static Connection getConnection() throws SQLException {
        if (!directorioListo) {
            asegurarDirectorio();
            directorioListo = true;
        }
        Connection conn = DriverManager.getConnection(URL_JDBC);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");   // en SQLite es por-conexión
            stmt.execute("PRAGMA busy_timeout = 5000"); // esperar al otro escritor
            stmt.execute("PRAGMA journal_mode = WAL");  // persistente; re-aplicar es inocuo
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        return conn;
    }

    /** Ruta del archivo .db — usada por el servicio de respaldos. */
    public static Path getRutaBaseDatos() {
        return DIR_DATOS.resolve("gymmanager.db");
    }

    /** Crea el directorio de datos si no existe todavía */
    private static void asegurarDirectorio() throws SQLException {
        try {
            Files.createDirectories(DIR_DATOS);
        } catch (IOException e) {
            throw new SQLException("No se pudo crear el directorio de datos: " + DIR_DATOS, e);
        }
    }

    /** Clase de utilidad estática, no instanciar */
    private DatabaseConnection() {}
}
