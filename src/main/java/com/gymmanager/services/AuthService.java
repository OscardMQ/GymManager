package com.gymmanager.services;

import com.gymmanager.dao.UsuarioDAO;
import com.gymmanager.dao.UsuarioDAOImpl;
import com.gymmanager.models.LoginRequest;
import com.gymmanager.models.Usuario;
import com.gymmanager.security.PasswordHasher;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Servicio de autenticación.
 * Valida credenciales con BCrypt y mantiene la referencia del usuario en sesión.
 */
public class AuthService {

    private static AuthService instancia;

    private final UsuarioDAO usuarioDAO;
    private Usuario usuarioActual;

    private AuthService() {
        this.usuarioDAO = new UsuarioDAOImpl();
    }

    public static AuthService getInstance() {
        if (instancia == null) {
            instancia = new AuthService();
        }
        return instancia;
    }

    /**
     * Valida las credenciales contra la BD.
     * Retorna el usuario autenticado, o vacío si las credenciales son inválidas.
     */
    public Optional<Usuario> autenticar(LoginRequest request) {
        if (request.getUsuario().isBlank() || request.getContrasena().isBlank()) {
            return Optional.empty();
        }

        try {
            Optional<Usuario> encontrado = usuarioDAO.buscarPorNombre(request.getUsuario());

            if (encontrado.isEmpty()) return Optional.empty();

            Usuario usuario = encontrado.get();

            if (!PasswordHasher.verificar(request.getContrasena(), usuario.getContrasenaBcrypt())) {
                return Optional.empty();
            }

            this.usuarioActual = usuario;
            return Optional.of(usuario);

        } catch (SQLException e) {
            System.err.println("[AuthService] Error de BD: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** Usuario con sesión activa. Null si nadie está logueado. */
    public Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /** Limpia la sesión al cerrar. */
    public void cerrarSesion() {
        this.usuarioActual = null;
    }
}