package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Pago;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite de PagoDAO.
 * Nunca cierra la conexión (es persistente y compartida).
 */
public class PagoDAOImpl implements PagoDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void guardar(Pago pago) throws SQLException {
        String sql = """
            INSERT INTO pagos (socio_id, fecha, monto, tipo_membresia_id)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt   (1, pago.getSocioId());
            ps.setString(2, pago.getFecha());
            ps.setDouble(3, pago.getMonto());
            ps.setInt   (4, pago.getTipoMembresiaId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) pago.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public List<Pago> listarDelMes(int anio, int mes) throws SQLException {
        // SQLite almacena fechas como texto ISO; strftime extrae año y mes
        String sql = """
            SELECT * FROM pagos
            WHERE strftime('%Y', fecha) = ?
              AND strftime('%m', fecha) = ?
        """;
        List<Pago> lista = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, String.format("%04d", anio));
            ps.setString(2, String.format("%02d", mes));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Pago p = new Pago();
                p.setId            (rs.getInt   ("id"));
                p.setSocioId       (rs.getInt   ("socio_id"));
                p.setFecha         (rs.getString("fecha"));
                p.setMonto         (rs.getDouble("monto"));
                p.setTipoMembresiaId(rs.getInt  ("tipo_membresia_id"));
                lista.add(p);
            }
        }
        return lista;
    }
}