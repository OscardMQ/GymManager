package com.gymmanager.services;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.utils.FechaUtils;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
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

    // ── Consultas — añadir antes del cierre de la clase ──────────────────────

    /**
     * Retorna todos los registros de la bitácora, del más reciente al más antiguo.
     * Cada Object[] contiene: [0]=fecha, [1]=hora, [2]=usuario, [3]=accion, [4]=descripcion
     */
    public List<Object[]> listar() {
        return listarConFiltros(null, null, null);
    }

    /**
     * Retorna registros filtrados por rango de fechas y/o tipo de acción.
     * Parámetros nulos o "Todas" no aplican filtro.
     */
    public List<Object[]> listarConFiltros(String desde, String hasta, String accion) {
        List<Object[]> lista = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
                "SELECT fecha, hora, usuario, accion, descripcion FROM bitacora WHERE 1=1"
        );
        List<String> params = new ArrayList<>();

        if (desde != null && !desde.isBlank()) {
            sql.append(" AND fecha >= ?");
            params.add(desde);
        }
        if (hasta != null && !hasta.isBlank()) {
            sql.append(" AND fecha <= ?");
            params.add(hasta);
        }
        if (accion != null && !accion.isBlank() && !"Todas".equals(accion)) {
            sql.append(" AND accion = ?");
            params.add(accion);
        }
        sql.append(" ORDER BY fecha DESC, hora DESC");

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setString(i + 1, params.get(i));
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        lista.add(new Object[]{
                                rs.getString("fecha"),
                                rs.getString("hora"),
                                rs.getString("usuario"),
                                rs.getString("accion"),
                                rs.getString("descripcion")
                        });
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[Bitácora] Error al consultar registros: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Retorna la lista de tipos de acción distintos presentes en la bitácora.
     * Se usa para poblar el ChoiceBox de filtros en BitacoraController.
     */
    public List<String> listarAcciones() {
        List<String> acciones = new ArrayList<>();
        String sql = "SELECT DISTINCT accion FROM bitacora ORDER BY accion";

        try {
            Connection conn = DatabaseConnection.getInstance().getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    acciones.add(rs.getString("accion"));
                }
            }
        } catch (SQLException e) {
            System.err.println("[Bitácora] Error al listar acciones: " + e.getMessage());
        }

        return acciones;
    }
}
