package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;

import java.sql.*;
import java.util.Optional;

/**
 * Implementación de ConfiguracionDAO sobre SQLite.
 * Usa INSERT OR REPLACE para upsert nativo sin lógica extra.
 */
public class ConfiguracionDAOImpl implements ConfiguracionDAO {

    @Override
    public Optional<String> get(String clave) throws SQLException {
        String sql = "SELECT valor FROM configuracion WHERE clave = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clave);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next()
                        ? Optional.ofNullable(rs.getString("valor"))
                        : Optional.empty();
            }
        }
    }

    @Override
    public void set(String clave, String valor) throws SQLException {
        String sql = "INSERT OR REPLACE INTO configuracion (clave, valor) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, clave);
            stmt.setString(2, valor);
            stmt.executeUpdate();
        }
    }
}