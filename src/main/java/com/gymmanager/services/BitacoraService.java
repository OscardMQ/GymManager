package com.gymmanager.services;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.utils.FechaUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Registra acciones relevantes del sistema en la tabla bitacora.
 * Acciones esperadas: LOGIN, LOGIN_FALLIDO, LOGOUT, ALTA_SOCIO, etc.
 */
public class BitacoraService {

    private static BitacoraService instancia;

    private static final String SQL_INSERTAR =
            "INSERT INTO bitacora (usuario, fecha, hora, accion, descripcion) VALUES (?, ?, ?, ?, ?)";

    private BitacoraService() {}

    public static BitacoraService getInstance() {
        if (instancia == null) {
            instancia = new BitacoraService();
        }
        return instancia;
    }

    /**
     * Inserta un registro en la bitácora.
     * Falla silenciosamente para no interrumpir el flujo principal.
     */
    public void registrar(String usuario, String accion, String descripcion) {
        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();

            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR)) {
                ps.setString(1, usuario);
                ps.setString(2, FechaUtils.hoyISO());
                ps.setString(3, FechaUtils.horaActual());
                ps.setString(4, accion);
                ps.setString(5, descripcion);
                ps.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("[Bitácora] Error al registrar acción '" + accion + "': " + e.getMessage());
        }
    }
}
