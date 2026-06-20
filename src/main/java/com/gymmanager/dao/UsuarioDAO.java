package com.gymmanager.dao;

import com.gymmanager.models.Usuario;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Contrato de acceso a datos para la tabla 'usuarios'.
 * Implementación: UsuarioDAOImpl — Fase 2.
 */
public interface UsuarioDAO {

    /** Busca un usuario por su nombre de acceso. Retorna vacío si no existe. */
    Optional<Usuario> buscarPorNombre(String usuario) throws SQLException;

    /** Persiste un nuevo usuario en la BD */
    void guardar(Usuario usuario) throws SQLException;

    /** Activa o desactiva un usuario (baja lógica) */
    void cambiarEstado(int id, boolean activo) throws SQLException;

    /** Actualiza la contraseña hasheada de un usuario */
    void actualizarContrasena(int id, String nuevaContrasenaBcrypt) throws SQLException;
}
