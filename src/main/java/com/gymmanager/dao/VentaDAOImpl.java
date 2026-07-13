package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.DetalleVenta;
import com.gymmanager.models.Venta;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación SQLite de VentaDAO.
 *
 * PATRÓN: Obtiene Connection por método mediante getConnection() (nunca como campo).
 *
 * guardar() usa una transacción explícita para garantizar atomicidad:
 *   INSERT ventas → obtiene id generado
 *   INSERT detalle_ventas × N (batch)
 *   UPDATE productos SET stock = stock - cantidad × N (batch)
 * Si cualquier paso falla, hace rollback completo.
 */
public class VentaDAOImpl implements VentaDAO {

    @Override
    public Venta guardar(Venta venta, List<DetalleVenta> detalles) throws SQLException {
        // Conexión propia: la transacción no puede chocar con otros hilos
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // ── 1. Insertar encabezado de la venta ──
                String sqlVenta = """
                    INSERT INTO ventas (fecha, hora, usuario, total)
                    VALUES (?, ?, ?, ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sqlVenta, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, venta.getFecha());
                    ps.setString(2, venta.getHora());
                    ps.setString(3, venta.getUsuario());
                    ps.setDouble(4, venta.getTotal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            venta.setId(rs.getInt(1));
                        }
                    }
                }

                // ── 2. Insertar renglones de detalle (batch) ──
                String sqlDetalle = """
                    INSERT INTO detalle_ventas (venta_id, producto_id, cantidad, precio_unitario)
                    VALUES (?, ?, ?, ?)
                    """;
                try (PreparedStatement ps = conn.prepareStatement(sqlDetalle)) {
                    for (DetalleVenta d : detalles) {
                        ps.setInt(1, venta.getId());
                        ps.setInt(2, d.getProductoId());
                        ps.setInt(3, d.getCantidad());
                        ps.setDouble(4, d.getPrecioUnitario());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                // ── 3. Descontar stock de cada producto (batch, misma transacción) ──
                // La condición stock >= cantidad evita stock negativo si el inventario
                // cambió entre la validación del service y este UPDATE.
                String sqlStock = "UPDATE productos SET stock = stock - ? WHERE id = ? AND stock >= ?";
                try (PreparedStatement ps = conn.prepareStatement(sqlStock)) {
                    for (DetalleVenta d : detalles) {
                        ps.setInt(1, d.getCantidad());
                        ps.setInt(2, d.getProductoId());
                        ps.setInt(3, d.getCantidad());
                        ps.addBatch();
                    }
                    int[] afectados = ps.executeBatch();
                    for (int i = 0; i < afectados.length; i++) {
                        if (afectados[i] == 0) {
                            throw new SQLException("Stock insuficiente para el producto ID "
                                    + detalles.get(i).getProductoId() + "; venta cancelada.");
                        }
                    }
                }

                conn.commit();
                return venta;

            } catch (SQLException e) {
                // Revertir todo si algo falla
                conn.rollback();
                throw e;
            }
        }
    }

    @Override
    public List<Venta> listarRecientes(int dias) throws SQLException {
        List<Venta> lista = new ArrayList<>();

        // Calcular fecha de corte en Java (patrón del proyecto, no SQL)
        String fechaCorte = LocalDate.now().minusDays(dias).toString();

        String sql = "SELECT * FROM ventas WHERE fecha >= ? ORDER BY fecha DESC, hora DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, fechaCorte);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    /** Construye un objeto Venta a partir del ResultSet actual. */
    private Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setId(rs.getInt("id"));
        v.setFecha(rs.getString("fecha"));
        v.setHora(rs.getString("hora"));
        v.setUsuario(rs.getString("usuario"));
        v.setTotal(rs.getDouble("total"));
        return v;
    }
}