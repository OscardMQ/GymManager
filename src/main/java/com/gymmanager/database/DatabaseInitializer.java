package com.gymmanager.database;

import com.gymmanager.security.PasswordHasher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Crea todas las tablas de la BD y carga los datos semilla.
 * Se ejecuta una vez al arrancar la app; los CREATE TABLE son idempotentes.
 */
public class DatabaseInitializer {

    public static void inicializar() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                crearTablas(stmt);
            }
            insertarDatosSemilla(conn);

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

        // Agregar dentro de inicializar(), junto a las demás tablas:
        String sqlProductos = """
    CREATE TABLE IF NOT EXISTS productos (
        id           INTEGER PRIMARY KEY AUTOINCREMENT,
        nombre       TEXT    NOT NULL,
        precio       REAL    NOT NULL DEFAULT 0,
        stock        INTEGER NOT NULL DEFAULT 0,
        stock_minimo INTEGER NOT NULL DEFAULT 5,
        categoria    TEXT    NOT NULL DEFAULT 'General'
    )
    """;
        stmt.execute(sqlProductos);

        // ── Tabla ventas: encabezado de cada venta ──
        stmt.execute("""
    CREATE TABLE IF NOT EXISTS ventas (
        id      INTEGER PRIMARY KEY AUTOINCREMENT,
        fecha   TEXT    NOT NULL,
        hora    TEXT    NOT NULL,
        usuario TEXT    NOT NULL,
        total   REAL    NOT NULL DEFAULT 0
    )
""");

// ── Tabla detalle_ventas: renglones de cada venta ──
        stmt.execute("""
    CREATE TABLE IF NOT EXISTS detalle_ventas (
        id              INTEGER PRIMARY KEY AUTOINCREMENT,
        venta_id        INTEGER NOT NULL,
        producto_id     INTEGER NOT NULL,
        cantidad        INTEGER NOT NULL DEFAULT 1,
        precio_unitario REAL    NOT NULL DEFAULT 0,
        FOREIGN KEY (venta_id)    REFERENCES ventas(id),
        FOREIGN KEY (producto_id) REFERENCES productos(id)
    )
""");

        // ── Índices para las consultas frecuentes (listados y reportes) ──
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_socios_fecha_fin      ON socios(fecha_fin)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_pagos_fecha           ON pagos(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_ventas_fecha          ON ventas(fecha)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_detalle_ventas_venta  ON detalle_ventas(venta_id)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_notificaciones_fecha  ON notificaciones(fecha_envio)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_bitacora_fecha        ON bitacora(fecha, hora)");
    }

    private static void insertarDatosSemilla(Connection conn) throws SQLException {

        try (Statement stmt = conn.createStatement()) {
            // Membresías base del gimnasio Gen Fit
            stmt.execute("""
                    INSERT OR IGNORE INTO membresias (id, nombre, precio, duracion_dias, descuento_estudiante, descripcion)
                    VALUES
                        (1, 'Mensual',     370.0, 30, 0, 'Acceso completo por 30 días'),
                        (2, 'Estudiante',  320.0, 30, 1, 'Mensual con descuento estudiantil'),
                        (3, 'Semanal',     120.0,  7, 0, 'Acceso por 7 días'),
                        (4, 'Visita',       40.0,  1, 0, 'Acceso por un día')
                    """);

            // Desactiva el 'admin' de semillas anteriores: su hash no correspondía
            // a la contraseña documentada, así que la cuenta era inutilizable.
            // Se filtra por el hash exacto para no tocar un 'admin' creado a mano.
            stmt.execute("""
                    UPDATE usuarios SET activo = 0
                    WHERE usuario = 'admin'
                      AND contrasena_bcrypt = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
                    """);
        }

        // Usuario administrador por defecto (contraseña: Admin123*).
        // Se verifica existencia antes de insertar para no recalcular
        // el hash BCrypt (~300 ms) en cada arranque.
        try (PreparedStatement check = conn.prepareStatement(
                "SELECT 1 FROM usuarios WHERE usuario = ?")) {
            check.setString(1, "Omar");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) return;
            }
        }

        try (PreparedStatement insert = conn.prepareStatement(
                "INSERT INTO usuarios (usuario, contrasena_bcrypt, rol, activo) VALUES (?, ?, 'ADMIN', 1)")) {
            insert.setString(1, "Omar");
            insert.setString(2, PasswordHasher.hashear("Admin123*"));
            insert.executeUpdate();
        }
    }
}
