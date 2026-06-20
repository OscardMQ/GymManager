package com.gymmanager.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea todas las tablas de la BD y carga los datos semilla.
 * Se ejecuta una vez al arrancar la app; los CREATE TABLE son idempotentes.
 */
public class DatabaseInitializer {

    public static void inicializar() {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            crearTablas(stmt);
            insertarDatosSemilla(stmt);

        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] Error al inicializar BD: " + e.getMessage());
            throw new RuntimeException("No se pudo inicializar la base de datos.", e);
        }
    }

    private static void crearTablas(Statement stmt) throws SQLException {

        // ── Usuarios del sistema ──────────────────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS usuarios (
                    id               INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario          TEXT NOT NULL UNIQUE,
                    contrasena_bcrypt TEXT NOT NULL,
                    rol              TEXT NOT NULL CHECK(rol IN ('ADMIN','RECEPCIONISTA')),
                    activo           INTEGER NOT NULL DEFAULT 1
                )
                """);

        // ── Catálogo de membresías ────────────────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS membresias (
                    id                   INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre               TEXT NOT NULL,
                    precio               REAL NOT NULL,
                    duracion_dias        INTEGER NOT NULL,
                    descuento_estudiante INTEGER NOT NULL DEFAULT 0,
                    descripcion          TEXT
                )
                """);

        // ── Socios del gimnasio ───────────────────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS socios (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre          TEXT NOT NULL,
                    telefono        TEXT,
                    whatsapp        TEXT,
                    tipo_membresia  INTEGER REFERENCES membresias(id),
                    fecha_inicio    TEXT NOT NULL,
                    fecha_fin       TEXT NOT NULL,
                    activo          INTEGER NOT NULL DEFAULT 1
                )
                """);

        // ── Bitácora de acciones ──────────────────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS bitacora (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    usuario     TEXT NOT NULL,
                    fecha       TEXT NOT NULL,
                    hora        TEXT NOT NULL,
                    accion      TEXT NOT NULL,
                    descripcion TEXT
                )
                """);

        // ── Pagos registrados (Fase 4) ────────────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS pagos (
                    id                 INTEGER PRIMARY KEY AUTOINCREMENT,
                    socio_id           INTEGER NOT NULL REFERENCES socios(id),
                    fecha              TEXT NOT NULL,
                    monto              REAL NOT NULL,
                    tipo_membresia_id  INTEGER REFERENCES membresias(id)
                )
                """);

        // ── Historial de notificaciones WhatsApp (Fase 5) ─────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS notificaciones (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    socio_id    INTEGER REFERENCES socios(id),
                    tipo        TEXT NOT NULL,
                    mensaje     TEXT NOT NULL,
                    fecha_envio TEXT NOT NULL,
                    estado      TEXT NOT NULL CHECK(estado IN ('ENVIADO','ERROR','SIN_WHATSAPP'))
                )
                """);

        // ── Configuración clave/valor (Fase 5) ────────────────────────────────
        stmt.execute("""
                CREATE TABLE IF NOT EXISTS configuracion (
                    clave TEXT PRIMARY KEY,
                    valor TEXT
                )
                """);
    }

    private static void insertarDatosSemilla(Statement stmt) throws SQLException {

        // Membresías base del gimnasio Gen Fit
        stmt.execute("""
                INSERT OR IGNORE INTO membresias (id, nombre, precio, duracion_dias, descuento_estudiante, descripcion)
                VALUES
                    (1, 'Mensual',     370.0, 30, 0, 'Acceso completo por 30 días'),
                    (2, 'Estudiante',  320.0, 30, 1, 'Mensual con descuento estudiantil'),
                    (3, 'Semanal',     120.0,  7, 0, 'Acceso por 7 días'),
                    (4, 'Visita',       40.0,  1, 0, 'Acceso por un día')
                """);

        // Usuario administrador por defecto (contraseña: Admin123*)
        // Hash BCrypt pre-generado para no depender de jBCrypt en la semilla
        stmt.execute("""
                INSERT OR IGNORE INTO usuarios (usuario, contrasena_bcrypt, rol, activo)
                VALUES ('admin',
                        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
                        'ADMIN', 1)
                """);
    }
}