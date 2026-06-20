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
}