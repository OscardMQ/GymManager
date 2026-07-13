package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación SQLite de ProductoDAO.
 * Cada operación abre y cierra su propia conexión (try-with-resources).
 */
public class ProductoDAOImpl implements ProductoDAO {

    @Override
    public List<Producto> listar() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos ORDER BY nombre";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("[ProductoDAOImpl] Error al listar productos: " + e.getMessage());
        }
        return lista;
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        String sql = "SELECT * FROM productos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductoDAOImpl] Error al buscar producto por ID: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public void guardar(Producto p) throws SQLException {
        String sql = "INSERT INTO productos (nombre, precio, stock, stock_minimo, categoria) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) p.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void actualizar(Producto p) throws SQLException {
        String sql = "UPDATE productos SET nombre=?, precio=?, stock=?, stock_minimo=?, "
                + "categoria=? WHERE id=?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, p);
            ps.setInt(6, p.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM productos WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Producto> listarStockBajo() {
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos WHERE stock <= stock_minimo "
                + "ORDER BY stock ASC, nombre ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("[ProductoDAOImpl] Error al listar stock bajo: " + e.getMessage());
        }
        return lista;
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getDouble("precio"),
                rs.getInt("stock"),
                rs.getInt("stock_minimo"),
                rs.getString("categoria")
        );
    }

    /** Parámetros 1-5 compartidos entre INSERT y UPDATE (sin el id). */
    private void bindParams(PreparedStatement ps, Producto p) throws SQLException {
        ps.setString(1, p.getNombre());
        ps.setDouble(2, p.getPrecio());
        ps.setInt   (3, p.getStock());
        ps.setInt   (4, p.getStockMinimo());
        ps.setString(5, p.getCategoria());
    }
}
