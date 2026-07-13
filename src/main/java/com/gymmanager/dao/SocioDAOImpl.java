package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Socio;
import java.sql.*;
import java.util.*;

public class SocioDAOImpl implements SocioDAO {

    private static final String SQL_BASE =
            "SELECT s.id, s.nombre, s.telefono, s.whatsapp, s.tipo_membresia, " +
                    "       s.fecha_inicio, s.fecha_fin, s.activo, m.nombre AS membresia_nombre " +
                    "FROM socios s LEFT JOIN membresias m ON s.tipo_membresia = m.id ";

    @Override
    public List<Socio> listar() throws SQLException {
        List<Socio> lista = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SQL_BASE + "ORDER BY s.nombre COLLATE NOCASE");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        }
        return lista;
    }

    @Override
    public List<Socio> buscarPorNombre(String nombre) throws SQLException {
        List<Socio> lista = new ArrayList<>();
        String sql = SQL_BASE + "WHERE LOWER(s.nombre) LIKE LOWER(?) ORDER BY s.nombre COLLATE NOCASE";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        }
        return lista;
    }

    @Override
    public void guardar(Socio socio) throws SQLException {
        String sql = "INSERT INTO socios (nombre, telefono, whatsapp, tipo_membresia, " +
                "fecha_inicio, fecha_fin, activo) VALUES (?, ?, ?, ?, ?, ?, 1)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParametrosComunes(ps, socio);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) socio.setId(keys.getInt(1));
            }
        }
    }

    @Override
    public void actualizar(Socio socio) throws SQLException {
        String sql = "UPDATE socios SET nombre=?, telefono=?, whatsapp=?, " +
                "tipo_membresia=?, fecha_inicio=?, fecha_fin=?, activo=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParametrosComunes(ps, socio);
            ps.setInt(7, socio.isActivo() ? 1 : 0);
            ps.setInt(8, socio.getId());
            ps.executeUpdate();
        }
    }

    @Override
    public void cambiarEstado(int id, boolean activo) throws SQLException {
        String sql = "UPDATE socios SET activo=? WHERE id=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private void setParametrosComunes(PreparedStatement ps, Socio s) throws SQLException {
        ps.setString(1, s.getNombre());
        ps.setString(2, s.getTelefono() != null ? s.getTelefono() : "");
        ps.setString(3, s.getWhatsapp()  != null ? s.getWhatsapp()  : "");
        ps.setInt   (4, s.getTipoMembresia());
        ps.setString(5, s.getFechaInicio());
        ps.setString(6, s.getFechaFin());
    }

    private Socio mapear(ResultSet rs) throws SQLException {
        Socio s = new Socio();
        s.setId(rs.getInt("id"));
        s.setNombre(rs.getString("nombre"));
        s.setTelefono(rs.getString("telefono"));
        s.setWhatsapp(rs.getString("whatsapp"));
        s.setTipoMembresia(rs.getInt("tipo_membresia"));
        s.setFechaInicio(rs.getString("fecha_inicio"));
        s.setFechaFin(rs.getString("fecha_fin"));
        s.setActivo(rs.getInt("activo") == 1);
        s.setMembresiaNombre(rs.getString("membresia_nombre"));
        return s;
    }
}
