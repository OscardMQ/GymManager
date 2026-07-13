package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Notificacion;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC de NotificacionDAO.
 * El JOIN con socios resuelve el nombre en una sola query (sin N+1).
 */
public class NotificacionDAOImpl implements NotificacionDAO {

    @Override
    public void guardar(Notificacion n) throws SQLException {
        String sql = """
                INSERT INTO notificaciones (socio_id, tipo, mensaje, fecha_envio, estado)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, n.getSocioId());
            stmt.setString(2, n.getTipo());
            stmt.setString(3, n.getMensaje());
            stmt.setString(4, n.getFechaEnvio());
            stmt.setString(5, n.getEstado());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) n.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public List<Notificacion> listarPorSocio(int socioId) throws SQLException {
        String sql = """
                SELECT n.*, s.nombre AS nombre_socio
                FROM notificaciones n
                LEFT JOIN socios s ON n.socio_id = s.id
                WHERE n.socio_id = ?
                ORDER BY n.fecha_envio DESC
                """;
        return ejecutarQuery(sql, stmt -> stmt.setInt(1, socioId));
    }

    @Override
    public List<Notificacion> listarRecientes(int dias) throws SQLException {
        // El corte se calcula en Java para evitar dependencia de funciones de fecha en SQLite
        String cutoff = LocalDate.now().minusDays(dias).toString(); // yyyy-MM-dd
        String sql = """
                SELECT n.*, s.nombre AS nombre_socio
                FROM notificaciones n
                LEFT JOIN socios s ON n.socio_id = s.id
                WHERE n.fecha_envio >= ?
                ORDER BY n.fecha_envio DESC
                """;
        return ejecutarQuery(sql, stmt -> stmt.setString(1, cutoff));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Reutiliza el bloque try-with-resources para las dos consultas de listado. */
    private List<Notificacion> ejecutarQuery(String sql, StatementPreparador preparador)
            throws SQLException {
        List<Notificacion> lista = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            preparador.preparar(stmt);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    private Notificacion mapear(ResultSet rs) throws SQLException {
        Notificacion n = new Notificacion();
        n.setId(rs.getInt("id"));
        n.setSocioId(rs.getInt("socio_id"));
        n.setTipo(rs.getString("tipo"));
        n.setMensaje(rs.getString("mensaje"));
        n.setFechaEnvio(rs.getString("fecha_envio"));
        n.setEstado(rs.getString("estado"));
        n.setNombreSocio(rs.getString("nombre_socio")); // null si socio_id = 0 (sistema)
        return n;
    }

    @FunctionalInterface
    private interface StatementPreparador {
        void preparar(PreparedStatement stmt) throws SQLException;
    }
}