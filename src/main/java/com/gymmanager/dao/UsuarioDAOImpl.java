package com.gymmanager.dao;

import com.gymmanager.database.DatabaseConnection;
import com.gymmanager.models.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Implementación SQLite de UsuarioDAO.
 * No cierra la Connection — la gestiona DatabaseConnection como singleton persistente.
 */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String SQL_BUSCAR =
            "SELECT id, usuario, contrasena_bcrypt, rol, activo " +
                    "FROM usuarios WHERE usuario = ? AND activo = 1";

    private static final String SQL_GUARDAR =
            "INSERT INTO usuarios (usuario, contrasena_bcrypt, rol, activo) VALUES (?, ?, ?, ?)";

    private static final String SQL_CAMBIAR_ESTADO =
            "UPDATE usuarios SET activo = ? WHERE id = ?";

    private static final String SQL_ACTUALIZAR_CONTRASENA =
            "UPDATE usuarios SET contrasena_bcrypt = ? WHERE id = ?";

    /** Retorna el usuario solo si existe y está activo. */
    @Override
    public Optional<Usuario> buscarPorNombre(String nombreUsuario) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR)) {
            ps.setString(1, nombreUsuario);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearResultado(rs));
                }
            }
        }

        return Optional.empty();
    }

    /** Inserta un nuevo usuario. El id lo asigna SQLite (autoincrement). */
    @Override
    public void guardar(Usuario usuario) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(SQL_GUARDAR)) {
            ps.setString(1, usuario.getUsuario());
            ps.setString(2, usuario.getContrasenaBcrypt());
            ps.setString(3, usuario.getRol().name());
            ps.setInt(4, usuario.isActivo() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    /** Baja/alta lógica — no elimina físicamente el registro. */
    @Override
    public void cambiarEstado(int id, boolean activo) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(SQL_CAMBIAR_ESTADO)) {
            ps.setInt(1, activo ? 1 : 0);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Reemplaza el hash BCrypt almacenado (cambio de contraseña). */
    @Override
    public void actualizarContrasena(int id, String nuevaContrasenaBcrypt) throws SQLException {
        Connection conn = DatabaseConnection.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR_CONTRASENA)) {
            ps.setString(1, nuevaContrasenaBcrypt);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    /** Mapea una fila del ResultSet a un objeto Usuario. */
    private Usuario mapearResultado(ResultSet rs) throws SQLException {
        Usuario u = new Usuario();
        u.setId(rs.getInt("id"));
        u.setUsuario(rs.getString("usuario"));
        u.setContrasenaBcrypt(rs.getString("contrasena_bcrypt"));
        u.setRol(Usuario.Rol.valueOf(rs.getString("rol")));
        u.setActivo(rs.getInt("activo") == 1);
        return u;
    }
}
