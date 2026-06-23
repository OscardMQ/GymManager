package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Membresia;
import java.sql.*;
import java.util.*;

public class MembresiaDAOImpl implements MembresiaDAO {

    private Connection con() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public List<Membresia> listar() throws SQLException {
        List<Membresia> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, precio, duracion_dias, descuento_estudiante, descripcion " +
                "FROM membresias ORDER BY precio";
        try (PreparedStatement ps = con().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    @Override
    public Optional<Membresia> buscarPorId(int id) throws SQLException {
        String sql = "SELECT id, nombre, precio, duracion_dias, descuento_estudiante, descripcion " +
                "FROM membresias WHERE id = ?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    private Membresia mapear(ResultSet rs) throws SQLException {
        Membresia m = new Membresia();
        m.setId(rs.getInt("id"));
        m.setNombre(rs.getString("nombre"));
        m.setPrecio(rs.getDouble("precio"));
        m.setDuracionDias(rs.getInt("duracion_dias"));
        m.setDescuentoEstudiante(rs.getDouble("descuento_estudiante"));
        m.setDescripcion(rs.getString("descripcion"));
        return m;
    }

    /** Inserta una nueva membresía en la BD. */
    public void insertar(Membresia m) throws SQLException {
        String sql =
                "INSERT INTO membresias (nombre, precio, duracion_dias, descuento_estudiante, descripcion) " +
                        "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setDouble(2, m.getPrecio());
            ps.setInt(3,    m.getDuracionDias());
            ps.setDouble(4, m.getDescuentoEstudiante());
            ps.setString(5, m.getDescripcion());
            ps.executeUpdate();
        }
    }

    /** Actualiza todos los campos de una membresía existente por su ID. */
    public void actualizar(Membresia m) throws SQLException {
        String sql =
                "UPDATE membresias SET nombre=?, precio=?, duracion_dias=?, " +
                        "descuento_estudiante=?, descripcion=? WHERE id=?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setString(1, m.getNombre());
            ps.setDouble(2, m.getPrecio());
            ps.setInt(3,    m.getDuracionDias());
            ps.setDouble(4, m.getDescuentoEstudiante());
            ps.setString(5, m.getDescripcion());
            ps.setInt(6,    m.getId());
            ps.executeUpdate();
        }
    }

    /** Elimina una membresía de la BD por su ID. */
    public void eliminar(int id) throws SQLException {
        String sql = "DELETE FROM membresias WHERE id=?";
        try (PreparedStatement ps = con().prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}